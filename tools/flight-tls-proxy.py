#!/usr/bin/env python3
"""Test-only TLS termination for a runner-owned plaintext Flight fixture. No HTTP/SQL rewriting."""
import argparse
import asyncio
import json
from pathlib import Path
import signal
import ssl


async def serve(backend_port, certificate, key, ready):
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(certificate, key)
    context.set_alpn_protocols(["h2"])
    stopped = asyncio.Event()
    loop = asyncio.get_running_loop()
    loop.add_signal_handler(signal.SIGTERM, stopped.set)
    loop.add_signal_handler(signal.SIGINT, stopped.set)

    async def forward(reader, writer):
        while chunk := await reader.read(65536):
            writer.write(chunk)
            await writer.drain()

    async def connected(reader, writer):
        backend = None
        tasks = []
        try:
            remote, backend = await asyncio.open_connection("127.0.0.1", backend_port)
            tasks = [asyncio.create_task(forward(reader, backend)), asyncio.create_task(forward(remote, writer))]
            _, pending = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
            for task in pending:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)
        except (ConnectionError, OSError):
            pass
        finally:
            for task in tasks:
                task.cancel()
            for stream in (writer, backend):
                if stream is not None:
                    stream.close()
                    try:
                        await stream.wait_closed()
                    except (ConnectionError, OSError):
                        pass

    server = await asyncio.start_server(connected, "127.0.0.1", 0, ssl=context, ssl_handshake_timeout=5)
    async with server:
        temporary = ready.with_suffix(".partial")
        temporary.write_text(json.dumps({"port": server.sockets[0].getsockname()[1], "backend": backend_port, "alpn": "h2"}))
        temporary.replace(ready)
        await stopped.wait()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend-port", type=int, required=True)
    parser.add_argument("--certificate", type=Path, required=True)
    parser.add_argument("--key", type=Path, required=True)
    parser.add_argument("--ready", type=Path, required=True)
    args = parser.parse_args()
    if not 0 < args.backend_port < 65536:
        parser.error("Invalid loopback backend port")
    asyncio.run(serve(args.backend_port, args.certificate, args.key, args.ready))
