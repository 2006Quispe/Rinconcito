# Ejecutar Rinconcito en otra PC

Se necesita JDK 21 o superior, MySQL 8 y Git. Python 3 solo es necesario
para autorizar Hotmail o usar el buzon de pruebas. Maven descarga las dependencias;
no se copian las carpetas target ni .m2 desde otra laptop.

## Base completa actual del proyecto

Para instalar el proyecto actualizado, importa una sola vez `rinconcito_db.sql` desde la raiz del repositorio en MySQL Workbench. El archivo crea `rinconcito_db` con las 28 tablas y los datos ficticios de demostracion necesarios; no ejecutes despues migraciones ni semillas individuales.

La importacion requiere una base vacia; el archivo no borra tablas existentes. Si ya tienes una `rinconcito_db` con tablas, importa en otra instancia/esquema limpio o respalda y retira el esquema anterior antes de importar. Configura `.local/application.properties` con el nombre `rinconcito_db` y las credenciales MySQL de tu propia laptop.

La importacion incluye cuentas demo `admin@rinconcito.test`, `mesero@rinconcito.test`, `caja@rinconcito.test` y `cliente@rinconcito.test`; la clave de prueba es `Prueba123!`. Son cuentas ficticias solo para desarrollo local.

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

Para una instalacion nueva, utiliza el archivo canónico `rinconcito_db.sql`; no hace
falta importar además las semillas o migraciones históricas de `src/main/resources/db/`.
Si usas una base existente, conserva tus datos y aplica únicamente las migraciones
que correspondan a la función que quieras habilitar.

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
| Caja | caja@rinconcito.test | Prueba123! |
| Mozo | mesero@rinconcito.test | Prueba123! |
| Administrador | admin@rinconcito.test | Prueba123! |

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
