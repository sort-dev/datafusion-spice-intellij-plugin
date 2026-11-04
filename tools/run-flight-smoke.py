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
import xml.etree.ElementTree as ET

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
        "spiceai/spiceai:2.4.0", "--http", "0.0.0.0:8090", "--flight", "0.0.0.0:50051", "--cpu-cores", "2",
        "--pods-watcher-enabled", *tls_args)
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


@contextlib.contextmanager
def tls_transport(backend):
    with tempfile.TemporaryDirectory(prefix="datafusion-tls-", dir=OUTPUT) as temporary:
        fixture = Path(temporary)
        for label in ("server", "untrusted"):
            run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                "-subj", f"/CN=disposable-{label}", "-addext", "subjectAltName=IP:127.0.0.1",
                "-keyout", str(fixture / f"{label}.key"), "-out", str(fixture / f"{label}.crt"),
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        ready = fixture / "ready.json"
        with (OUTPUT / "datafusion-tls-proxy.log").open("w") as log:
            process = subprocess.Popen(["python3", str(ROOT / "tools/flight-tls-proxy.py"), "--backend-port", str(backend),
                "--certificate", str(fixture / "server.crt"), "--key", str(fixture / "server.key"), "--ready", str(ready)],
                stdout=log, stderr=subprocess.STDOUT)
            try:
                deadline = time.monotonic() + 10
                while not ready.is_file():
                    if process.poll() is not None:
                        raise RuntimeError("Disposable TLS proxy exited during startup")
                    if time.monotonic() >= deadline:
                        raise TimeoutError("Disposable TLS proxy did not become ready")
                    time.sleep(0.1)
                yield json.loads(ready.read_text())["port"], fixture
            finally:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


def test(product, port, java_home, secure_fixture=None, token=None, ide_process=False, test_task="test", spicepod=None,
         credential_phase=None, credential_directory=None, credential_driver=None, lifecycle_probe=False, cancellation_probe=False,
         data_grip_ide=None):
    env = os.environ.copy()
    env.update(JAVA_HOME=str(java_home), BRIKK_FLIGHT_TEST_PRODUCT=product,
               BRIKK_FLIGHT_TEST_URL=f"jdbc:arrow-flight-sql://127.0.0.1:{port}/?useEncryption=false")
    if spicepod:
        env["BRIKK_FLIGHT_TEST_SPICEPOD"] = str(spicepod)
    else:
        env.pop("BRIKK_FLIGHT_TEST_SPICEPOD", None)
    label = f"{product}-secure" if secure_fixture else product
    if ide_process:
        label += f"-remote-{test_task}"
    if lifecycle_probe:
        label += "-shared-race"
        env["BRIKK_FLIGHT_TEST_LIFECYCLE_PROBE"] = "true"
    else:
        env.pop("BRIKK_FLIGHT_TEST_LIFECYCLE_PROBE", None)
    if cancellation_probe:
        label += f"-cancellation-{test_task}"
        env["BRIKK_FLIGHT_TEST_CANCELLATION_PROBE"] = "true"
    else:
        env.pop("BRIKK_FLIGHT_TEST_CANCELLATION_PROBE", None)
    if credential_phase:
        label += f"-saved-{credential_phase}"
        env.update(BRIKK_FLIGHT_CREDENTIAL_PHASE=credential_phase,
                   BRIKK_FLIGHT_CREDENTIAL_DIRECTORY=str(credential_directory),
                   BRIKK_FLIGHT_CREDENTIAL_DRIVER=credential_driver)
    else:
        for key in ("BRIKK_FLIGHT_CREDENTIAL_PHASE", "BRIKK_FLIGHT_CREDENTIAL_DIRECTORY", "BRIKK_FLIGHT_CREDENTIAL_DRIVER"):
            env.pop(key, None)
    if secure_fixture:
        env.update(BRIKK_FLIGHT_TEST_URL=f"jdbc:arrow-flight-sql://127.0.0.1:{port}/?useEncryption=true",
                   BRIKK_FLIGHT_TEST_TLS_ROOT=str(secure_fixture / "server.crt"),
                   BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT=str(secure_fixture / "untrusted.crt"))
        if token:
            env["BRIKK_FLIGHT_TEST_TOKEN"] = token
        else:
            env.pop("BRIKK_FLIGHT_TEST_TOKEN", None)
    else:
        for key in ("BRIKK_FLIGHT_TEST_TLS_ROOT", "BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT", "BRIKK_FLIGHT_TEST_TOKEN"):
            env.pop(key, None)
    started = time.time()
    status = OUTPUT / f"{label}-live-status.json"
    status.write_text(json.dumps({"product": product, "status": "running", "started": started}))
    success = False
    try:
        with (OUTPUT / f"{label}-live-tests.log").open("w") as log:
            classes = ["FlightSqlServerTest", "FlightNamespaceContractTest"]
            if secure_fixture:
                classes.append("FlightSqlSecurityTest" if product == "spice" else "FlightTlsContractTest")
            if ide_process:
                classes.append("RemoteFlightProcessTest")
            if credential_phase:
                classes = ["RemoteFlightProcessTest.testSavedPasswordSafeCredentialsReloadForConsoleAndHelperConnections"]
            if lifecycle_probe:
                classes = ["SharedFlightCatalogRaceTest"]
            if cancellation_probe:
                classes = ["FlightCancellationTest"]
            filters = [argument for name in classes for argument in ("--tests", f"dev.sort.datafusionspice.live.{name}")]
            target = [f"-Ptest.dataGripIde={data_grip_ide}"] if data_grip_ide else []
            run(str(ROOT / "gradlew"), test_task, *target, *filters,
                "--no-daemon", "--max-workers=2", "--console=plain", cwd=ROOT, env=env,
                stdout=log, stderr=subprocess.STDOUT, timeout=45 if cancellation_probe else 300)
        reports = [report for report in (ROOT / "build/test-results" / test_task).glob("TEST-*Flight*Test.xml")
                   if report.stat().st_mtime >= started]
        if not reports:
            raise RuntimeError("Live task produced no fresh XML; cached or skipped execution is not qualification")
        for report in reports:
            suite = ET.parse(report).getroot()
            if int(suite.get("tests", "0")) == 0 or any(int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")):
                raise RuntimeError(f"Live task has incomplete or failing evidence: {report.name}")
        success = True
    finally:
        status.write_text(json.dumps({"product": product, "status": "passed" if success else "failed", "started": started}))
        destination = OUTPUT / f"{label}-test-results"
        destination.mkdir(exist_ok=True)
        for report in (ROOT / "build/test-results" / test_task).glob("TEST-*Flight*Test.xml"):
            if report.stat().st_mtime >= started:
                shutil.copyfile(report, destination / report.name)


def exercise(product, port, args, fixture=None, token=None, secure=False):
    options = dict(secure_fixture=fixture if secure else None, token=token,
                   ide_process=args.ide_process, test_task=args.test_task,
                   spicepod=fixture / "spicepod.yaml" if product == "spice" else None,
                   lifecycle_probe=args.shared_lifecycle_probe, cancellation_probe=args.cancellation_probe,
                   data_grip_ide=args.data_grip_ide)
    if args.saved_credentials_restart:
        with tempfile.TemporaryDirectory(prefix="saved-flight-", dir=OUTPUT) as directory:
            driver = f"credential-fixture-{secrets.token_hex(12)}"
            for phase in ("write", "read"):
                test(product, port, args.java_home, **options, credential_phase=phase,
                     credential_directory=Path(directory), credential_driver=driver)
    else:
        test(product, port, args.java_home, **options)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", choices=["spice", "datafusion", "both"], default="both")
    parser.add_argument("--spice-secure", action="store_true", help="Use disposable TLS certificates and API-key authentication for Spice")
    parser.add_argument("--datafusion-tls", action="store_true", help="Terminate TLS in a bounded test proxy in front of the unmodified DataFusion fixture")
    parser.add_argument("--ide-process", action="store_true", help="Also launch the IDE's real remote JDBC process")
    parser.add_argument("--test-task", choices=["test", "test262", "test263", "testDataGrip"], default="test")
    parser.add_argument("--data-grip-ide", type=Path, help="Read-only DataGrip install for the dedicated SQL-only consumer lane")
    parser.add_argument("--saved-credentials-restart", action="store_true", help="Write/read an isolated encrypted credential store in separate test JVMs")
    parser.add_argument("--shared-lifecycle-probe", action="store_true", help="Run desired-contract race reproducers; failures remain release blockers")
    parser.add_argument("--cancellation-probe", action="store_true", help="Run one bounded streaming-cancel/recovery check under a 45-second outer process limit")
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--datafusion-binary", type=Path,
                        default=ROOT / "build/datafusion-reference/debug/brikk-datafusion-flight-execution")
    args = parser.parse_args()
    if args.saved_credentials_restart and not args.ide_process:
        parser.error("--saved-credentials-restart requires --ide-process")
    if args.shared_lifecycle_probe and (not args.ide_process or args.saved_credentials_restart):
        parser.error("--shared-lifecycle-probe requires --ide-process and cannot be combined with credential restart")
    if args.cancellation_probe and (args.shared_lifecycle_probe or args.saved_credentials_restart):
        parser.error("--cancellation-probe cannot be combined with other probe modes")
    if (args.test_task == "testDataGrip") != (args.data_grip_ide is not None):
        parser.error("testDataGrip requires --data-grip-ide, and that path is only used by testDataGrip")
    if args.data_grip_ide is not None:
        info = json.loads((args.data_grip_ide / "product-info.json").read_text())
        if info.get("productCode") != "DB":
            parser.error("--data-grip-ide must identify a DataGrip installation")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if args.product in ("spice", "both"):
        with spice(args.spice_secure) as (port, fixture, token):
            exercise("spice", port, args, fixture, token, args.spice_secure)
    if args.product in ("datafusion", "both"):
        with datafusion(args.datafusion_binary) as port:
            if args.datafusion_tls:
                with tls_transport(port) as (encrypted_port, fixture):
                    exercise("datafusion", encrypted_port, args, fixture, secure=True)
            else:
                exercise("datafusion", port, args)
