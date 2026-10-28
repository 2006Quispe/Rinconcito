"""Prueba con datos ficticios. Solo llama a la app 8082 y al buzon local 8025."""
import base64
import datetime
import http.cookiejar
import json
import re
import time
import urllib.parse
import urllib.request
import uuid
from html.parser import HTMLParser
from pathlib import Path

APP = 'http://localhost:8082'
BOX = 'http://localhost:8025'


class Inputs(HTMLParser):
    def __init__(self):
        super().__init__()
        self.values = {}

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'input' and 'name' in attrs:
            self.values[attrs['name']] = attrs.get('value', '')


class Cliente:
    def __init__(self):
        self.web = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.token = ''

    def leer(self, response):
        content = response.read().decode()
        inputs = Inputs()
        inputs.feed(content)
        self.token = inputs.values.get('_csrf', self.token)
        return response.url, content

    def get(self, path):
        return self.leer(self.web.open(APP + path, timeout=20))

    def post(self, path, values):
        return self.leer(self.web.open(APP + path, urllib.parse.urlencode({**values, '_csrf': self.token}).encode(), timeout=20))

    def pago(self, codigo, operacion, ruta="/pedidos/pago"):
        boundary = 'Rinconcito' + uuid.uuid4().hex
        data = bytearray()
        fields = {'_csrf': self.token, 'codigo': codigo, 'metodoPago': 'YAPE', 'numeroOperacion': operacion}
        for key, value in fields.items():
            data.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
        data.extend(f'--{boundary}\r\nContent-Disposition: form-data; name="captura"; filename="prueba.png"\r\nContent-Type: image/png\r\n\r\n'.encode())
        data.extend(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jP0kAAAAASUVORK5CYII='))
        data.extend(f'\r\n--{boundary}--\r\n'.encode())
        req = urllib.request.Request(APP + ruta, bytes(data), {'Content-Type': 'multipart/form-data; boundary=' + boundary})
        return self.leer(self.web.open(req, timeout=20))


def mensajes():
    return json.load(urllib.request.urlopen(BOX + '/api/mensajes', timeout=10))


def esperar(predicate, segundos=35):
    limite = time.monotonic() + segundos
    while time.monotonic() < limite:
        found = next((msg for msg in mensajes() if predicate(msg)), None)
        if found:
            return found
        time.sleep(1)
    raise AssertionError('El mensaje esperado no aparecio en el buzon local.')


def main():
    stamp = str(int(time.time()))
    dni = str(80000000 + int(stamp[-6:]))
    destino = f'pedido.{stamp}@example.test'
    cliente = Cliente()
    cliente.get('/carta')
    cliente.post('/carrito/agregar', {'idProducto': 2, 'cantidad': 2})
    cliente.get('/pedidos')
    url, page = cliente.post('/pedidos', {'tipoEntrega': 'RECOJO_LOCAL', 'dni': dni, 'nombres': 'Prueba',
                    'apellidos': 'Correo Local', 'telefono': '900123456', 'correo': destino})
    if '/pedidos/pago?codigo=' not in url:
        Path('.local/error-pedido.html').write_text(page, encoding='utf-8')
        raise AssertionError('No se creo el pedido. Revisa .local/error-pedido.html')
    codigo = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['codigo'][0]
    resumen = esperar(lambda m: codigo in m['asunto'] and not m['adjuntos'])
    assert resumen['para'] == destino and 'PENDIENTE_PAGO' in resumen['texto']
    assert '2 x Pachamanca' in resumen['texto'] and 'S/ 90' in resumen['texto']
    print('Pedido registrado y resumen recibido:', codigo, flush=True)

    otro = Cliente()
    otro.get('/reservas')
    manana = (datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=-5))).date() + datetime.timedelta(days=1)).isoformat()
    _, page = otro.post('/reservas', {'dni': str(int(dni)+1), 'nombres': 'Reserva', 'apellidos': 'Correo Local',
           'telefono': '900123457', 'correo': f'reserva.{stamp}@example.test', 'fecha': manana,
           'horaInicio': '12:00', 'numeroPersonas': 2, 'idPiso': 1})
    reserva = esperar(lambda m: f'reserva.{stamp}@example.test' == m['para'])
    assert manana in reserva['texto']
    print('Reserva registrada y resumen recibido:', reserva['asunto'], flush=True)

    cliente.pago(codigo, 'CORREO-LOCAL-' + stamp)
    caja = Cliente()
    caja.get('/personal/login')
    url, _ = caja.post('/personal/login', {'correo': 'caja@rinconcito.pe', 'contrasena': 'Prueba123!'})
    assert '/caja/' in url, url
    _, pagos = caja.get('/caja/pedidos-pendientes')
    index = pagos.find(codigo)
    assert index >= 0, 'No aparece el pago del pedido de prueba.'
    match = re.search(r'/caja/pedidos-pendientes/(\d+)/aprobar', pagos[index:])
    assert match, 'No se encontro la accion de aprobacion.'
    caja.post(f'/caja/pedidos-pendientes/{match[1]}/aprobar', {})
    nota = esperar(lambda m: codigo in m['asunto'] and bool(m['adjuntos']))
    assert nota['para'] == destino and 'S/ 90' in nota['texto']
    pdf = urllib.request.urlopen(BOX + '/archivo?id=' + nota['id'] + '&parte=0').read()
    assert pdf.startswith(b'%PDF-') and len(pdf) > 500
    Path('.local/nota-ejemplo.pdf').write_bytes(pdf)
    print('Pago aprobado y nota PDF recibida:', nota['adjuntos'][0], len(pdf), 'bytes', flush=True)

    # Un fallo del SMTP se prueba mediante un reenvio explicito, sin alterar el pago.
    _, notas = caja.get('/caja/notas-venta')
    index = notas.find(codigo)
    assert index >= 0
    match = re.search(r'/caja/notas-venta/(\d+)/correo', notas[index:])
    assert match, 'No se encontro Enviar correo.'
    id_nota = match[1]
    urllib.request.urlopen(urllib.request.Request(BOX + '/api/smtp', b'{"activo":false}', {'Content-Type':'application/json'})).read()
    try:
        caja.post(f'/caja/notas-venta/{id_nota}/correo', {})
        limite = time.monotonic() + 20
        while time.monotonic() < limite:
            _, cola = caja.get('/caja/correos')
            if 'ERROR' in cola and codigo in cola:
                break
            time.sleep(1)
        else:
            raise AssertionError('No se registro el fallo de SMTP.')
    finally:
        urllib.request.urlopen(urllib.request.Request(BOX + '/api/smtp', b'{"activo":true}', {'Content-Type':'application/json'})).read()
    # El reenvio de la nota programa el intento y conserva una sola fila por documento.
    caja.post(f'/caja/notas-venta/{id_nota}/correo', {})
    previo = nota['id']
    esperar(lambda m: codigo in m['asunto'] and bool(m['adjuntos']) and m['id'] != previo)
    _, cola = caja.get('/caja/correos')
    assert 'ENVIADO' in cola
    print('SMTP pausado: error registrado. Reanudado: reenvio recibido. Pedido y pago conservados.', flush=True)
    Path('.local/resultado-prueba.json').write_text(json.dumps({'pedido':codigo,'destino':destino,'nota':id_nota,
       'confirmacion_pedido':True,'confirmacion_reserva':True,'pdf':True,'reintento':True},indent=2),encoding='utf-8')


if __name__ == '__main__':
    main()
