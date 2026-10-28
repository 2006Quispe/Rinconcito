"""Buzon SMTP de desarrollo. Escucha solo en este equipo; nunca reenvia correo."""
import asyncio
import email
import email.policy
import html
import json
import threading
import uuid
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs

ROOT = Path(__file__).resolve().parents[1] / '.local' / 'correos'
ROOT.mkdir(parents=True, exist_ok=True)
smtp_activo = True


async def smtp(reader, writer):
    writer.write(b'220 localhost SMTP de prueba\r\n')
    await writer.drain()
    message = []
    data = False
    sender = False
    recipient = False
    try:
        while line := await reader.readline():
            if data:
                if line == b'.\r\n':
                    name = datetime.now().strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex
                    (ROOT / (name + '.eml')).write_bytes(b''.join(message))
                    message = []
                    data = sender = recipient = False
                    writer.write(b'250 Capturado en buzon local\r\n')
                else:
                    message.append(line[1:] if line.startswith(b'..') else line)
            else:
                command = line.decode(errors='replace').strip().upper()
                if command.startswith(('EHLO', 'HELO')):
                    writer.write(b'250-localhost\r\n250 SIZE 10000000\r\n')
                elif command.startswith('MAIL FROM:'):
                    sender = smtp_activo
                    writer.write(b'250 OK\r\n' if smtp_activo else b'451 SMTP pausado para prueba de reintento\r\n')
                elif command.startswith('RCPT TO:'):
                    recipient = sender
                    writer.write(b'250 OK\r\n' if sender else b'503 Falta MAIL\r\n')
                elif command == 'DATA':
                    if sender and recipient:
                        data = True
                        writer.write(b'354 Terminar con punto\r\n')
                    else:
                        writer.write(b'503 Falta destinatario\r\n')
                elif command == 'QUIT':
                    writer.write(b'221 Hasta luego\r\n')
                    await writer.drain()
                    break
                elif command == 'RSET':
                    sender = recipient = False
                    writer.write(b'250 OK\r\n')
                elif command == 'NOOP':
                    writer.write(b'250 OK\r\n')
                else:
                    writer.write(b'502 No disponible\r\n')
            await writer.drain()
    finally:
        writer.close()
        await writer.wait_closed()


def mensajes():
    result = []
    for path in sorted(ROOT.glob('*.eml'), reverse=True):
        message = email.message_from_bytes(path.read_bytes(), policy=email.policy.default)
        body = message.get_body(preferencelist=('plain',))
        result.append({'id': path.stem, 'para': str(message['To']), 'de': str(message['From']),
                       'asunto': str(message['Subject']), 'texto': body.get_content() if body else '',
                       'adjuntos': [str(p.get_filename()) for p in message.iter_attachments()]})
    return result


class Web(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def responder(self, body, content_type='text/html; charset=utf-8', status=200):
        if isinstance(body, str):
            body = body.encode()
        self.send_response(status)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        global smtp_activo
        if self.path == '/api/smtp':
            body = self.rfile.read(int(self.headers.get('Content-Length', 0)))
            smtp_activo = bool(json.loads(body)['activo'])
            self.responder(json.dumps({'activo': smtp_activo}), 'application/json')
        else:
            self.responder('No encontrado', status=404)

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == '/api/mensajes':
            self.responder(json.dumps(mensajes(), ensure_ascii=False), 'application/json; charset=utf-8')
            return
        if parsed.path == '/archivo':
            values = parse_qs(parsed.query)
            ident = values.get('id', [''])[0]
            found = next((p for p in ROOT.glob('*.eml') if p.stem == ident), None)
            if not found:
                self.responder('No encontrado', status=404)
                return
            message = email.message_from_bytes(found.read_bytes(), policy=email.policy.default)
            attachments = list(message.iter_attachments())
            try:
                part = attachments[int(values.get('parte', ['0'])[0])]
            except (IndexError, ValueError):
                self.responder('No encontrado', status=404)
                return
            self.responder(part.get_payload(decode=True), 'application/pdf')
            return
        parts = ['''<!doctype html><html lang="es"><meta charset="utf-8"><title>Buzón local - Rinconcito</title>
<style>body{font:16px Arial;background:#f8f4ed;color:#38271e;max-width:1050px;margin:35px auto;padding:20px}article{background:white;padding:22px;margin:20px 0;border:1px solid #ddd;border-radius:12px}a{color:#8b301d}pre{white-space:pre-wrap;font:15px Arial;line-height:1.6}button{padding:10px;background:#8b301d;color:white;border:0;border-radius:6px;cursor:pointer}</style>
<h1>Buzón de prueba de El Rinconcito</h1><p>Estos mensajes permanecen en tu equipo. No se envían a Gmail ni a otros destinatarios reales.</p>
<p><a href="http://localhost:8082">Abrir aplicación de prueba</a> · <a href="/">Actualizar buzón</a></p>''']
        parts.append('<p>Recepción SMTP: <strong>' + ('activa' if smtp_activo else 'pausada') + '</strong></p>')
        parts.append('<button onclick="fetch(\'/api/smtp\',{method:\'POST\',body:JSON.stringify({activo:'
                     + ('false' if smtp_activo else 'true') + '})}).then(()=>location.reload())">'
                     + ('Pausar recepción para probar errores' if smtp_activo else 'Reanudar recepción') + '</button>')
        entries = mensajes()
        if not entries:
            parts.append('<article>Todavía no hay correos. Crea un pedido o reserva en la aplicación de prueba.</article>')
        for msg in entries:
            parts.append('<article><h2>' + html.escape(msg['asunto']) + '</h2><p>Para: '
                         + html.escape(msg['para']) + ' · De: ' + html.escape(msg['de']) + '</p><pre>'
                         + html.escape(msg['texto']) + '</pre>')
            for index, name in enumerate(msg['adjuntos']):
                parts.append('<p><a target="_blank" href="/archivo?id=' + msg['id'] + '&parte=' + str(index)
                             + '">Abrir ' + html.escape(name) + '</a></p>')
            parts.append('</article>')
        self.responder(''.join(parts))


async def main():
    threading.Thread(target=ThreadingHTTPServer(('127.0.0.1', 8025), Web).serve_forever, daemon=True).start()
    server = await asyncio.start_server(smtp, '127.0.0.1', 1025)
    print('Buzon: http://localhost:8025 | SMTP: 127.0.0.1:1025', flush=True)
    async with server:
        await server.serve_forever()


if __name__ == '__main__':
    asyncio.run(main())
