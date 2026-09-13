#!/usr/bin/env python3
"""Small LAN-only collector/dashboard for Nuvio playback diagnostics.

The app POSTs JSON reports to /api/playback-reports. Reports are deliberately
stored as individual JSON files so they remain easy to inspect and delete.
This is intended for temporary debugging on a trusted home network, not as a
public internet service.
"""

from __future__ import annotations

import argparse
import html
import json
import os
import secrets
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

DEFAULT_PORT = 8787
DEFAULT_DATA_DIR = Path(__file__).resolve().parents[1] / "diagnostics-data"
MAX_BODY_BYTES = 8 * 1024 * 1024


def report_id() -> str:
    return f"{int(time.time() * 1000)}-{secrets.token_hex(4)}"


def json_response(handler: BaseHTTPRequestHandler, status: int, payload: object) -> None:
    encoded = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(encoded)))
    handler.send_header("Cache-Control", "no-store")
    handler.end_headers()
    handler.wfile.write(encoded)


class Handler(BaseHTTPRequestHandler):
    server_version = "NuvioDiagnostics/1.0"

    def log_message(self, fmt: str, *args: object) -> None:
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")

    @property
    def config(self):
        return self.server.config  # type: ignore[attr-defined]

    def authorized(self) -> bool:
        expected = self.config.token
        if not expected:
            return True
        supplied = self.headers.get("Authorization", "")
        return secrets.compare_digest(supplied, f"Bearer {expected}")

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            json_response(self, 200, {"ok": True, "service": "nuvio-playback-diagnostics"})
            return
        if not self.authorized():
            json_response(self, 401, {"error": "unauthorized"})
            return
        if path == "/":
            self.dashboard()
        elif path == "/api/reports":
            self.list_reports()
        elif path.startswith("/api/reports/"):
            self.get_report(unquote(path.rsplit("/", 1)[-1]))
        else:
            self.send_error(404)

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        report_kind = {
            "/api/playback-reports": "playback",
            "/api/auth-diagnostics": "auth",
        }.get(path)
        if report_kind is None:
            self.send_error(404)
            return
        if not self.authorized():
            json_response(self, 401, {"error": "unauthorized"})
            return
        length_header = self.headers.get("Content-Length")
        try:
            length = int(length_header or "0")
        except ValueError:
            length = 0
        if length <= 0 or length > MAX_BODY_BYTES:
            json_response(self, 413, {"error": "invalid or oversized body"})
            return
        try:
            raw = self.rfile.read(length)
            payload = json.loads(raw.decode("utf-8"))
            if not isinstance(payload, dict):
                raise ValueError("report must be a JSON object")
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as exc:
            json_response(self, 400, {"error": f"invalid JSON: {exc}"})
            return

        identifier = report_id()
        destination = self.config.data_dir / f"{identifier}.json"
        temporary = destination.with_suffix(".json.tmp")
        payload["_collector"] = {
            "reportId": identifier,
            "kind": report_kind,
            "receivedAtMs": int(time.time() * 1000),
            "clientAddress": self.client_address[0],
        }
        with self.config.write_lock:
            temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            temporary.replace(destination)
        print(f"stored {report_kind} report {identifier} ({length} bytes) -> {destination}")
        json_response(self, 201, {"id": identifier, "reportId": identifier})

    def list_reports(self) -> None:
        reports = []
        with self.config.write_lock:
            paths = sorted(self.config.data_dir.glob("*.json"), reverse=True)
            for path in paths[:200]:
                try:
                    data = json.loads(path.read_text(encoding="utf-8"))
                    reports.append({
                        "id": path.stem,
                        "receivedAtMs": data.get("_collector", {}).get("receivedAtMs"),
                        "app": data.get("app", {}),
                        "device": data.get("device", {}),
                        "stream": data.get("stream", {}),
                        "player": data.get("player", {}),
                        "error": data.get("error", {}),
                        "playbackAnalytics": data.get("playbackAnalytics", {}),
                    })
                except (OSError, json.JSONDecodeError):
                    continue
        json_response(self, 200, reports)

    def get_report(self, identifier: str) -> None:
        if not identifier or "/" in identifier or "\\" in identifier:
            self.send_error(400)
            return
        path = self.config.data_dir / f"{identifier}.json"
        if not path.is_file():
            self.send_error(404)
            return
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            self.send_error(500)
            return
        json_response(self, 200, payload)

    def dashboard(self) -> None:
        body = """<!doctype html>
<html><head><meta charset="utf-8"><title>Nuvio playback diagnostics</title>
<style>body{font:14px system-ui;background:#101114;color:#eee;margin:2rem}table{border-collapse:collapse;width:100%}td,th{padding:.5rem;border-bottom:1px solid #333;text-align:left}a{color:#8bc7ff}code{color:#b8e986}.muted{color:#999}</style></head>
<body><h1>Nuvio playback diagnostics</h1><p class="muted">Temporary local collector. Refreshes every 5 seconds. Reports are stored on this machine.</p>
<table><thead><tr><th>Received</th><th>Device</th><th>Stream host</th><th>Engine</th><th>Rebuffers</th><th>Bytes</th><th>Error</th><th></th></tr></thead><tbody id="rows"></tbody></table>
<script>
const esc=s=>String(s??'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));
async function load(){const r=await fetch('/api/reports',{cache:'no-store'});const a=await r.json();document.querySelector('#rows').innerHTML=a.map(x=>{const p=x.playbackAnalytics||{};const d=x.device||{};const s=x.stream||{};const e=x.error||{};return `<tr><td>${esc(new Date(x.receivedAtMs||0).toLocaleString())}</td><td>${esc([d.manufacturer,d.model].filter(Boolean).join(' '))}</td><td><code>${esc(s.host||'unknown')}</code></td><td>${esc(x.player?.engine||'')}</td><td>${esc(p.rebufferCount??'')} / ${esc(p.rebufferTotalMs??'')}ms</td><td>${esc(p.totalBytesLoaded??'')}</td><td>${esc(e.errorCodeName||e.exceptionClass||'')}</td><td><a href="/api/reports/${encodeURIComponent(x.id)}" target="_blank">JSON</a></td></tr>`}).join('')||'<tr><td colspan="8">No reports yet.</td></tr>'}load();setInterval(load,5000);
</script></body></html>"""
        encoded = body.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="0.0.0.0", help="bind address (default: all LAN interfaces)")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--data-dir", type=Path, default=DEFAULT_DATA_DIR)
    parser.add_argument("--token", default=os.environ.get("NUVIO_DIAGNOSTICS_TOKEN", ""))
    args = parser.parse_args()
    args.data_dir.mkdir(parents=True, exist_ok=True)
    config = type("Config", (), {"data_dir": args.data_dir, "token": args.token, "write_lock": threading.Lock()})()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    server.config = config  # type: ignore[attr-defined]
    print(f"Nuvio diagnostics listening on http://{args.host}:{args.port}")
    print(f"Dashboard: http://127.0.0.1:{args.port}/")
    print(f"Data directory: {args.data_dir}")
    if args.token:
        print("Bearer-token authentication: enabled")
    else:
        print("Bearer-token authentication: disabled; use only on a trusted LAN")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
