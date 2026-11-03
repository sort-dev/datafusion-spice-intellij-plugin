#!/usr/bin/env python3
"""Opt-in tests on runner-owned loopback fixtures, never saved IDE data sources."""
import argparse
import contextlib
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import ssl
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build/readiness"


def run(*args, **kwargs):
    return subprocess.run(args, check=True, text=True, **kwargs)


@contextlib.contextmanager
def spice(secure=False):
    if secure:
        with tempfile.TemporaryDirectory(prefix="spice-secure-", dir=OUTPUT) as directory:
            with spice_fixture(Path(directory), True) as endpoint:
                yield endpoint
    else:
        with spice_fixture(OUTPUT / "spice-fixture", False) as endpoint:
            yield endpoint


@contextlib.contextmanager
def spice_fixture(fixture, secure):
    fixture.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ROOT / "src/test/resources/live/spicepod.yaml", fixture / "spicepod.yaml")
    token = secrets.token_hex(32) if secure else None
    tls_args = []
    if secure:
        with (fixture / "spicepod.yaml").open("a") as pod:
            pod.write(f"runtime:\n  auth:\n    api-key:\n      enabled: true\n      keys:\n        - {token}\n")
        for label in ("server", "untrusted"):
            run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                "-subj", f"/CN=disposable-{label}", "-addext", "subjectAltName=IP:127.0.0.1",
                "-keyout", str(fixture / f"{label}.key"), "-out", str(fixture / f"{label}.crt"),
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        tls_args = ["--tls-enabled", "true", "--tls-certificate-file", "/app/server.crt",
                    "--tls-key-file", "/app/server.key"]
    name = f"datafusion-spice-readiness-{os.getpid()}"
    run("docker", "run", "--detach", "--name", name, "--cpus", "2", "--memory", "2g",
        "--user", "0:0", "--tmpfs", "/run/spice:rw,mode=1777",
        "--env", "SPICE_CONFIG_DIR=/run/spice",
        "--publish", "127.0.0.1::50051", "--publish", "127.0.0.1::8090",
        "--mount", f"type=bind,src={fixture},dst=/app,readonly", "--workdir", "/app",
        "spiceai/spiceai:2.4.0", "--http", "0.0.0.0:8090", "--flight", "0.0.0.0:50051", "--cpu-cores", "2", *tls_args)
    try:
        info = json.loads(run("docker", "inspect", name, capture_output=True).stdout)[0]
        ports = info["NetworkSettings"]["Ports"]
        flight = int(ports["50051/tcp"][0]["HostPort"])
        http = int(ports["8090/tcp"][0]["HostPort"])
        deadline = time.monotonic() + 60
        while True:
            try:
                context = ssl.create_default_context(cafile=str(fixture / "server.crt")) if secure else None
                scheme = "https" if secure else "http"
                with urllib.request.urlopen(f"{scheme}://127.0.0.1:{http}/v1/ready", timeout=1, context=context) as response:
                    if response.status == 200:
                        break
            except (urllib.error.URLError, OSError):
                pass
            state = json.loads(run("docker", "inspect", name, capture_output=True).stdout)[0]["State"]
            if not state["Running"]:
                raise RuntimeError("Disposable Spice fixture exited during startup; inspect its saved log")
            if time.monotonic() >= deadline:
                raise TimeoutError("Disposable Spice fixture did not become ready")
            time.sleep(0.2)
        (OUTPUT / "spice-image.json").write_text(json.dumps({"image": info["Config"]["Image"], "id": info["Image"]}, indent=2))
        yield flight, fixture, token
    finally:
        logs = subprocess.run(["docker", "logs", name], capture_output=True, text=True)
        log_text = logs.stdout + logs.stderr
        (OUTPUT / ("spice-secure-fixture.log" if secure else "spice-fixture.log")).write_text(
            log_text.replace(token, "[redacted]") if token else log_text)
        subprocess.run(["docker", "rm", "--force", name], check=False)


@contextlib.contextmanager
def datafusion(binary):
    binary = binary.resolve()
    if not binary.is_file():
        raise FileNotFoundError(f"Build the pinned upstream execution fixture first: {binary}")
    with socket.socket() as reservation:
        reservation.bind(("127.0.0.1", 0))
        port = reservation.getsockname()[1]
    with (OUTPUT / "datafusion-fixture.log").open("w") as log:
        process = subprocess.Popen([str(binary), str(port)], stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 30
            while True:
                if process.poll() is not None:
                    raise RuntimeError("DataFusion execution fixture exited during startup")
                try:
                    with socket.create_connection(("127.0.0.1", port), timeout=0.2):
                        break
                except OSError:
                    if time.monotonic() >= deadline:
                        raise TimeoutError("DataFusion fixture did not become ready")
                    time.sleep(0.1)
            yield port
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


def test(product, port, java_home, secure_fixture=None, token=None, ide_process=False, test_task="test"):
    env = os.environ.copy()
    env.update(JAVA_HOME=str(java_home), BRIKK_FLIGHT_TEST_PRODUCT=product,
               BRIKK_FLIGHT_TEST_URL=f"jdbc:arrow-flight-sql://127.0.0.1:{port}/?useEncryption=false")
    label = f"{product}-secure" if secure_fixture else product
    if ide_process:
        label += f"-remote-{test_task}"
    if secure_fixture:
        env.update(BRIKK_FLIGHT_TEST_URL=f"jdbc:arrow-flight-sql://127.0.0.1:{port}/?useEncryption=true",
                   BRIKK_FLIGHT_TEST_TLS_ROOT=str(secure_fixture / "server.crt"),
                   BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT=str(secure_fixture / "untrusted.crt"),
                   BRIKK_FLIGHT_TEST_TOKEN=token)
    else:
        for key in ("BRIKK_FLIGHT_TEST_TLS_ROOT", "BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT", "BRIKK_FLIGHT_TEST_TOKEN"):
            env.pop(key, None)
    started = time.time()
    status = OUTPUT / f"{label}-live-status.json"
    status.write_text(json.dumps({"product": product, "status": "running", "started": started}))
    success = False
    try:
        with (OUTPUT / f"{label}-live-tests.log").open("w") as log:
            run(str(ROOT / "gradlew"), test_task, "--tests", "dev.sort.datafusionspice.live.FlightSqlServerTest",
                *(["--tests", "dev.sort.datafusionspice.live.FlightSqlSecurityTest"] if secure_fixture else []),
                *(["--tests", "dev.sort.datafusionspice.live.RemoteFlightProcessTest"] if ide_process else []),
                "--no-daemon", "--max-workers=2", "--console=plain", cwd=ROOT, env=env,
                stdout=log, stderr=subprocess.STDOUT, timeout=300)
        success = True
    finally:
        status.write_text(json.dumps({"product": product, "status": "passed" if success else "failed", "started": started}))
        destination = OUTPUT / f"{label}-test-results"
        destination.mkdir(exist_ok=True)
        for report in (ROOT / "build/test-results" / test_task).glob("TEST-*Flight*Test.xml"):
            if report.stat().st_mtime >= started:
                shutil.copyfile(report, destination / report.name)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", choices=["spice", "datafusion", "both"], default="both")
    parser.add_argument("--spice-secure", action="store_true", help="Use disposable TLS certificates and API-key authentication for Spice")
    parser.add_argument("--ide-process", action="store_true", help="Also launch the IDE's real remote JDBC process")
    parser.add_argument("--test-task", choices=["test", "test262", "test263"], default="test")
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--datafusion-binary", type=Path,
                        default=ROOT / "build/datafusion-reference/debug/brikk-datafusion-flight-execution")
    args = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if args.product in ("spice", "both"):
        with spice(args.spice_secure) as (port, fixture, token):
            test("spice", port, args.java_home, fixture if args.spice_secure else None, token, args.ide_process, args.test_task)
    if args.product in ("datafusion", "both"):
        with datafusion(args.datafusion_binary) as port:
            test("datafusion", port, args.java_home, ide_process=args.ide_process, test_task=args.test_task)
