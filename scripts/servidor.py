#!/usr/bin/env python3
"""Servidor estatico com suporte a Range, para o ExoPlayer poder avancar no video
sem voltar a descarregar tudo do inicio."""
import os, re, sys, socketserver
from http.server import SimpleHTTPRequestHandler

RAIZ = sys.argv[1] if len(sys.argv) > 1 else "."
PORTA = int(sys.argv[2]) if len(sys.argv) > 2 else 8000

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=RAIZ, **kw)

    def send_head(self):
        intervalo = self.headers.get("Range")
        if not intervalo:
            return super().send_head()

        caminho = self.translate_path(self.path)
        if os.path.isdir(caminho):
            return super().send_head()
        try:
            f = open(caminho, "rb")
        except OSError:
            self.send_error(404)
            return None

        tamanho = os.fstat(f.fileno()).st_size
        m = re.match(r"bytes=(\d*)-(\d*)", intervalo.strip())
        if not m:
            f.close()
            self.send_error(400)
            return None
        inicio = int(m.group(1)) if m.group(1) else 0
        fim = int(m.group(2)) if m.group(2) else tamanho - 1
        fim = min(fim, tamanho - 1)
        if inicio > fim:
            f.close()
            self.send_response(416)
            self.send_header("Content-Range", f"bytes */{tamanho}")
            self.end_headers()
            return None

        self.send_response(206)
        self.send_header("Content-Type", self.guess_type(caminho))
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Range", f"bytes {inicio}-{fim}/{tamanho}")
        self.send_header("Content-Length", str(fim - inicio + 1))
        self.end_headers()
        f.seek(inicio)
        return LimitadoA(f, fim - inicio + 1)

    def end_headers(self):
        self.send_header("Accept-Ranges", "bytes")
        super().end_headers()

    def log_message(self, *a):
        pass

    def handle_one_request(self):
        # O leitor corta a ligacao a meio sempre que salta ou fecha o video.
        # Sem isto o traceback do Broken pipe enche o terminal.
        try:
            super().handle_one_request()
        except (BrokenPipeError, ConnectionResetError):
            self.close_connection = True

class LimitadoA:
    """Envolve o ficheiro para o copyfile parar no fim do intervalo pedido."""
    def __init__(self, f, restantes):
        self.f, self.restantes = f, restantes
    def read(self, n=-1):
        if self.restantes <= 0:
            return b""
        if n < 0 or n > self.restantes:
            n = self.restantes
        d = self.f.read(n)
        self.restantes -= len(d)
        return d
    def close(self):
        self.f.close()

class Servidor(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

print(f"a servir {RAIZ} em 0.0.0.0:{PORTA}", flush=True)
Servidor(("0.0.0.0", PORTA), Handler).serve_forever()
