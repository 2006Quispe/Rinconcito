# Ejecutar Rinconcito en otra PC

Se necesita JDK 21 o superior, MySQL 8 y Git. Python 3 solo es necesario
para autorizar Hotmail o usar el buzon de pruebas. Maven descarga las dependencias;
no se copian las carpetas target ni .m2 desde otra laptop.

## 1. Descargar y preparar MySQL

Si ya tienes el repositorio, guarda tus cambios locales antes de actualizar:

```powershell
git pull --ff-only origin main
```

Si no lo tienes:

```powershell
git clone https://github.com/2006Quispe/Rinconcito.git
cd Rinconcito
```

Para una demo nueva, abre en MySQL Workbench
`src/main/resources/db/demo_mozo/rinconcito_demo_mozo.sql` y ejecutalo UNA VEZ.
Crea la base independiente `rinconcito_demo_mozo`, sin borrar tu base anterior,
con productos, mesas, reservas, pedidos y cuentas ficticias. Ya incluye las tablas
actuales de historial de disponibilidad y correo.

Si usas una base existente, conserva tus datos. Para habilitar correo selecciona
esa base en Workbench y ejecuta `src/main/resources/db/migration_correo_salida.sql`.
El modo sin correo no consulta esa tabla. Esta migracion no crea el resto del sistema.

## 2. Configurar el MySQL de tu laptop

Desde la raiz del proyecto:

```powershell
New-Item -ItemType Directory -Force .local
Copy-Item docs/application-local.properties.example .local/application.properties
```

Edita `.local/application.properties`: coloca el nombre de tu base, tu usuario
MySQL y tu contrasena MySQL. El archivo queda fuera de Git. No reemplaces
`src/main/resources/application.properties` con las credenciales de otra persona.
Tambien se admiten DB_URL, DB_USERNAME y DB_PASSWORD si no usas el archivo local.

## 3. Arranque normal: funciona sin configurar correo

```powershell
.\mvnw.cmd spring-boot:run
```

Abre http://localhost:8081. Por defecto CORREO_HABILITADO es false: pedidos,
reservas, pagos y notas siguen funcionando; no se generan ni envian correos.
No hace falta cuenta Azure ni token. Si habias activado CORREO_HABILITADO en esa
terminal, quita esa variable antes del arranque normal.

Las cuentas de la base demo son:

| Rol | Correo | Clave |
| --- | --- | --- |
| Caja | caja@rinconcito.pe | Prueba123! |
| Mozo | mesero@rinconcito.pe | Prueba123! |
| Administrador | admin@rinconcito.pe | Prueba123! |

Acceso de personal: http://localhost:8081/personal/login.
Estas cuentas solo se crean al importar la demo.

## 4. Opcional: enviar desde el correo del negocio

No crees otra aplicacion Azure: la del equipo ya existe. Quien tenga acceso al
buzon del negocio autoriza cada laptop por separado:

```powershell
python tools/autorizar_outlook.py --client-id 0a3c222d-e0ce-45de-a2bd-9ac072cf749b --cuenta rinconcito.demo@hotmail.com
```

Abre la direccion indicada por el script, introduce el codigo, comprueba
Rinconcito Demo y acepta los permisos. No compartas la contrasena por GitHub.
Se guarda un token privado en `.local/outlook-oauth.properties`.

Deten la instancia normal y ejecuta:

```powershell
.\tools\Iniciar-Hotmail.ps1
```

Si PowerShell bloquea scripts, se puede ejecutar para este comando:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Iniciar-Hotmail.ps1
```

El script activa el perfil hotmail y el correo solo para ese proceso. Requiere
Internet, autorizacion valida y la tabla correo_salida en tu base. Tu laptop envia
directamente mediante Microsoft Graph, aunque la PC del autor este apagada.

## Comprobar el recorrido

1. Registra una cuenta cliente con un correo tuyo y crea un pedido.
2. El cliente registra su pago y adjunta el comprobante (datos ficticios para la demo).
3. Caja aprueba el pago y emite la nota de venta.
4. Se envia la confirmacion inicial y, al emitir la nota, el PDF del pago aprobado.
5. Revisa http://localhost:8081/caja/correos para ver errores o reintentar.

Si el proveedor falla durante un envio, la operacion queda guardada y el correo
puede reintentarse. ENVIADO significa que Microsoft acepto el mensaje; revisa
Recibidos y Spam. No habilites correo real en una cola con destinatarios de prueba pendientes.

Detalles del remitente: [CORREO_HOTMAIL.md](CORREO_HOTMAIL.md).
