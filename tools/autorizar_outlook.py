"""Autoriza la cuenta remitente en Microsoft. No pide ni guarda su contrasena."""
import argparse
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

BASE = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/'
SCOPE = 'https://graph.microsoft.com/Mail.Send offline_access'


def post(endpoint, data):
    request = urllib.request.Request(BASE + endpoint, urllib.parse.urlencode(data).encode(),
                                     {'Content-Type': 'application/x-www-form-urlencoded'})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        # Se utilizan codigos, no cuerpos completos que pudieran contener datos sensibles.
        try:
            return json.loads(error.read())
        except Exception:
            return {'error': 'http_' + str(error.code)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--client-id', default=os.environ.get('OUTLOOK_CLIENT_ID'))
    parser.add_argument('--cuenta', default=os.environ.get('SMTP_USERNAME'))
    args = parser.parse_args()
    if not args.client_id or not args.cuenta:
        parser.error('Indica --client-id ID_PUBLICO --cuenta correo@hotmail.com')
    try:
        uuid.UUID(args.client_id)
    except ValueError:
        parser.error('El Id. de aplicacion debe ser un UUID valido.')
    device = post('devicecode', {'client_id': args.client_id, 'scope': SCOPE})
    if 'device_code' not in device:
        raise SystemExit('Microsoft no pudo iniciar la autorizacion (' + str(device.get('error', 'desconocido'))
                         + '). Revisa las cuentas personales y los flujos de cliente publico de la aplicacion.')
    print('Autoriza SOLO tu aplicacion Rinconcito Demo y la cuenta ' + args.cuenta, flush=True)
    print('Abre ' + device['verification_uri'] + ' y escribe este codigo: ' + device['user_code'], flush=True)
    print('La contrasena se introduce unicamente en Microsoft; este programa no la solicita.', flush=True)
    intervalo = max(5, int(device.get('interval', 5)))
    limite = time.monotonic() + int(device.get('expires_in', 900))
    while time.monotonic() < limite:
        time.sleep(intervalo)
        result = post('token', {'client_id': args.client_id,
                               'grant_type': 'urn:ietf:params:oauth:grant-type:device_code',
                               'device_code': device['device_code']})
        if 'refresh_token' in result:
            target = Path(__file__).resolve().parents[1] / '.local' / 'outlook-oauth.properties'
            target.parent.mkdir(parents=True, exist_ok=True)
            temporal = target.with_name('outlook-' + uuid.uuid4().hex + '.tmp')
            try:
                contenido = 'client_id=' + args.client_id + '\nusername=' + args.cuenta + '\nrefresh_token=' + result['refresh_token'] + '\n'
                descriptor = os.open(temporal, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
                with os.fdopen(descriptor, 'w', encoding='utf-8') as output:
                    output.write(contenido)
                os.replace(temporal, target)
            finally:
                temporal.unlink(missing_ok=True)
            print('Autorizacion guardada en .local/outlook-oauth.properties; no compartas ese archivo.', flush=True)
            print('Ya puedes iniciar con el perfil hotmail y habilitar el correo.', flush=True)
            return
        codigo = result.get('error', 'respuesta_incompleta')
        if codigo == 'authorization_pending':
            continue
        if codigo == 'slow_down':
            intervalo += 5
            continue
        raise SystemExit('Autorizacion no completada (' + str(codigo) + ').')
    raise SystemExit('El codigo vencio. Ejecuta nuevamente el programa cuando estes listo para iniciar sesion.')


if __name__ == '__main__':
    main()
