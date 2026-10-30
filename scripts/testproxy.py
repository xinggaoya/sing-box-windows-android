"""端到端验证用的本地 HTTP CONNECT 代理 + 订阅文件服务器。

- 8899: HTTP CONNECT 代理（模拟代理节点），会打印每个 CONNECT 目标
- 8898: 订阅文件服务器，提供 /sub.json（sing-box JSON 订阅）
"""
import http.server
import json
import socket
import socketserver
import threading

PROXY_PORT = 8899
SUB_PORT = 8898

SUBSCRIPTION = {
    "outbounds": [
        {
            "type": "http",
            "tag": "测试节点",
            "server": "10.0.2.2",
            "server_port": PROXY_PORT,
        }
    ]
}


def pump(src, dst):
    try:
        while True:
            data = src.recv(65536)
            if not data:
                break
            dst.sendall(data)
    except Exception:
        pass
    finally:
        try:
            dst.shutdown(socket.SHUT_WR)
        except Exception:
            pass


def handle_proxy(conn):
    try:
        req = b""
        while b"\r\n\r\n" not in req:
            chunk = conn.recv(4096)
            if not chunk:
                return
            req += chunk
        line = req.split(b"\r\n")[0].decode("latin-1")
        parts = line.split(" ")
        method = parts[0]
        target = parts[1] if len(parts) > 1 else ""
        print("PROXY %s %s" % (method, target), flush=True)
        if method != "CONNECT":
            conn.sendall(b"HTTP/1.1 405 Method Not Allowed\r\n\r\n")
            return
        host, _, port = target.partition(":")
        upstream = socket.create_connection((host, int(port or 443)), timeout=20)
        conn.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
        t1 = threading.Thread(target=pump, args=(conn, upstream), daemon=True)
        t2 = threading.Thread(target=pump, args=(upstream, conn), daemon=True)
        t1.start()
        t2.start()
        t1.join()
        t2.join()
    except Exception as exc:  # noqa: BLE001
        print("PROXY-ERR %s" % exc, flush=True)
    finally:
        try:
            conn.close()
        except Exception:
            pass


class ProxyHandler(socketserver.BaseRequestHandler):
    def handle(self):
        handle_proxy(self.request)


class SubHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):  # noqa: N802
        body = json.dumps(SUBSCRIPTION, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


class Threaded(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True
    daemon_threads = True


if __name__ == "__main__":
    threading.Thread(
        target=Threaded(("0.0.0.0", PROXY_PORT), ProxyHandler).serve_forever, daemon=True
    ).start()
    print("proxy on %d, subscription on %d" % (PROXY_PORT, SUB_PORT), flush=True)
    Threaded(("0.0.0.0", SUB_PORT), SubHandler).serve_forever()
