import asyncio
import json
from pathlib import Path
import ssl
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class FlightTlsProxyTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        parent = ROOT / "build/tls-proxy-tests"
        parent.mkdir(parents=True, exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=parent)
        self.addCleanup(self.directory.cleanup)
        self.path = Path(self.directory.name)
        self.certificate = self.path / "server.crt"
        subprocess.run(["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
            "-subj", "/CN=disposable-proxy-test", "-addext", "subjectAltName=IP:127.0.0.1",
            "-keyout", str(self.path / "server.key"), "-out", str(self.certificate)],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.backend_connections = 0

        async def echo(reader, writer):
            self.backend_connections += 1
            try:
                while payload := await reader.read(65536):
                    writer.write(payload)
                    await writer.drain()
            finally:
                writer.close()
                await writer.wait_closed()

        self.backend = await asyncio.start_server(echo, "127.0.0.1", 0)
        self.addAsyncCleanup(self.close_backend)
        ready = self.path / "ready.json"
        self.proxy = await asyncio.create_subprocess_exec(sys.executable, str(ROOT / "tools/flight-tls-proxy.py"),
            "--backend-port", str(self.backend.sockets[0].getsockname()[1]), "--certificate", str(self.certificate),
            "--key", str(self.path / "server.key"), "--ready", str(ready),
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.addAsyncCleanup(self.close_proxy)
        async with asyncio.timeout(5):
            while not ready.is_file():
                if self.proxy.returncode is not None:
                    self.fail("TLS proxy exited before readiness")
                await asyncio.sleep(0.02)
        self.port = json.loads(ready.read_text())["port"]

    async def close_backend(self):
        self.backend.close()
        await self.backend.wait_closed()

    async def close_proxy(self):
        if self.proxy.returncode is None:
            self.proxy.terminate()
        await asyncio.wait_for(self.proxy.wait(), 5)

    def context(self):
        context = ssl.create_default_context(cafile=str(self.certificate))
        context.set_alpn_protocols(["h2"])
        return context

    async def testTlsAlpnAndBidirectionalBytesArePreserved(self):
        reader, writer = await asyncio.open_connection("127.0.0.1", self.port, ssl=self.context(), server_hostname="127.0.0.1")
        try:
            self.assertEqual("h2", writer.get_extra_info("ssl_object").selected_alpn_protocol())
            payload = b"PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n\x00fixture-bytes\xff"
            writer.write(payload)
            await writer.drain()
            self.assertEqual(payload, await asyncio.wait_for(reader.readexactly(len(payload)), 2))
        finally:
            writer.close()
            await writer.wait_closed()
        self.assertEqual(1, self.backend_connections)

    async def testUntrustedRootIsRejectedBeforeBackendConnection(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            await asyncio.open_connection("127.0.0.1", self.port, ssl=ssl.create_default_context(), server_hostname="127.0.0.1")
        self.assertEqual(0, self.backend_connections)

    async def testWrongHostnameIsRejectedBeforeBackendConnection(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            await asyncio.open_connection("127.0.0.1", self.port, ssl=self.context(), server_hostname="localhost")
        self.assertEqual(0, self.backend_connections)
