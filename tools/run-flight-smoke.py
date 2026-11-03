#!/usr/bin/env python3
"""Opt-in tests on runner-owned loopback fixtures, never saved IDE data sources."""
import argparse
import contextlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build/readiness"


def run(*args, **kwargs):
    return subprocess.run(args, check=True, text=True, **kwargs)


@contextlib.contextmanager
def spice():
    fixture = OUTPUT / "spice-fixture"
    fixture.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ROOT / "src/test/resources/live/spicepod.yaml", fixture / "spicepod.yaml")
    name = f"datafusion-spice-readiness-{os.getpid()}"
    run("docker", "run", "--rm", "--detach", "--name", name, "--cpus", "2", "--memory", "2g",
        "--publish", "127.0.0.1::50051", "--publish", "127.0.0.1::8090",
        "--mount", f"type=bind,src={fixture / 'spicepod.yaml'},dst=/app/spicepod.yaml,readonly", "--workdir", "/app",
        "spiceai/spiceai:2.4.0", "--http", "0.0.0.0:8090", "--flight", "0.0.0.0:50051", "--cpu-cores", "2")
    try:
        info = json.loads(run("docker", "inspect", name, capture_output=True).stdout)[0]
        ports = info["NetworkSettings"]["Ports"]
        flight = int(ports["50051/tcp"][0]["HostPort"])
        http = int(ports["8090/tcp"][0]["HostPort"])
        deadline = time.monotonic() + 60
        while True:
            try:
                with urllib.request.urlopen(f"http://127.0.0.1:{http}/v1/ready", timeout=1) as response:
                    if response.status == 200:
                        break
            except (urllib.error.URLError, OSError):
                pass
            if time.monotonic() >= deadline:
                raise TimeoutError("Disposable Spice fixture did not become ready")
            time.sleep(0.2)
        (OUTPUT / "spice-image.json").write_text(json.dumps({"image": info["Config"]["Image"], "id": info["Image"]}, indent=2))
        yield flight
    finally:
        logs = subprocess.run(["docker", "logs", name], capture_output=True, text=True)
        (OUTPUT / "spice-fixture.log").write_text(logs.stdout + logs.stderr)
        subprocess.run(["docker", "stop", "-t", "10", name], check=False)


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


def test(product, port, java_home):
    env = os.environ.copy()
    env.update(JAVA_HOME=str(java_home), BRIKK_FLIGHT_TEST_PRODUCT=product,
               BRIKK_FLIGHT_TEST_URL=f"jdbc:arrow-flight-sql://127.0.0.1:{port}/?useEncryption=false")
    started = time.time()
    status = OUTPUT / f"{product}-live-status.json"
    status.write_text(json.dumps({"product": product, "status": "running", "started": started}))
    success = False
    try:
        with (OUTPUT / f"{product}-live-tests.log").open("w") as log:
            run(str(ROOT / "gradlew"), "test", "--tests", "dev.sort.datafusionspice.live.FlightSqlServerTest",
                "--no-daemon", "--max-workers=2", "--console=plain", cwd=ROOT, env=env,
                stdout=log, stderr=subprocess.STDOUT, timeout=300)
        success = True
    finally:
        status.write_text(json.dumps({"product": product, "status": "passed" if success else "failed", "started": started}))
        destination = OUTPUT / f"{product}-test-results"
        destination.mkdir(exist_ok=True)
        for report in (ROOT / "build/test-results/test").glob("TEST-*FlightSqlServerTest.xml"):
            if report.stat().st_mtime >= started:
                shutil.copyfile(report, destination / report.name)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", choices=["spice", "datafusion", "both"], default="both")
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--datafusion-binary", type=Path,
                        default=ROOT / "build/datafusion-reference/debug/brikk-datafusion-flight-execution")
    args = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if args.product in ("spice", "both"):
        with spice() as port:
            test("spice", port, args.java_home)
    if args.product in ("datafusion", "both"):
        with datafusion(args.datafusion_binary) as port:
            test("datafusion", port, args.java_home)
