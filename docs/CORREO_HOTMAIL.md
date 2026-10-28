# Remitente de Hotmail para el demo

Hotmail/Outlook.com requiere autenticación moderna OAuth2. Esta integración usa
SMTP por `smtp-mail.outlook.com:587`, STARTTLS y XOAUTH2. No usa ni guarda la
contraseña normal del correo. La cuenta que autoriza el envío debe ser la misma
que se configura como remitente.

## 1. Registrar la aplicación una vez

En [Microsoft Entra](https://entra.microsoft.com/), abre **Registros de aplicaciones
→ Nuevo registro**. Nombre: `Rinconcito Demo`. Elige un tipo de cuenta que incluya
**cuentas personales de Microsoft**. No hace falta un URI de redirección para el
flujo de código de dispositivo utilizado aquí.

En **Autenticación**, habilita los flujos de cliente público. En **Permisos de API**,
añade **Office 365 Exchange Online → Permisos delegados → SMTP.Send**. No elijas
permisos de aplicación ni crees un secreto de cliente para este flujo de demo.

Copia el **Id. de aplicación (cliente)** de Información general. Ese ID es público;
la contraseña y los tokens de autorización no lo son. Si tu cuenta no permite
registrar aplicaciones, hace falta resolver ese acceso en Microsoft antes de
poder habilitar este remitente.

## 2. Autorizar en cada laptop que enviará mensajes

Desde la carpeta del proyecto:

```powershell
python tools/autorizar_outlook.py --client-id TU_ID_PUBLICO --cuenta rinconcitodemo@hotmail.com
```

El programa muestra un código y te dirige a `https://microsoft.com/devicelogin`.
Inicia sesión allí con la cuenta del negocio y acepta el permiso de envío de tu
aplicación. La contraseña se introduce solo en Microsoft. Si el código vence,
ejecuta otra vez el programa.

La autorización queda en `.local/outlook-oauth.properties`, ignorada por Git. No
copies ese archivo en el repositorio ni lo compartas por chat. En la otra laptop
se hace su propia autorización. El programa renueva automáticamente los tokens;
si Microsoft revoca el acceso, vuelve a ejecutar el comando.

## 3. Preparar la base y habilitar el envío

Aplica `src/main/resources/db/migration_correo_salida.sql` en la base del demo.
En este equipo la tabla ya fue preparada en `rinconcito_demo_mozo`.

Crea `.local/hotmail.properties` con las tres propiedades locales de conexión
`spring.datasource.url`, `spring.datasource.username` y
`spring.datasource.password`, copiadas de tu configuración actual. No subas este
archivo a Git. Si necesitas otro puerto, añade `server.port`.

Detén la instancia que use ese puerto **y la copia de prueba en 8082**. Ambas
comparten la cola: debe haber un solo procesador activo para evitar que el buzón
simulado capture mensajes destinados a Hotmail. Después ejecuta:

```powershell
.\tools\Iniciar-Hotmail.ps1
```

El script configura el remitente, habilita el correo y arranca con el perfil
`hotmail`. **No usa `.local/correo.properties`**, que pertenece al buzón simulado.
No se activa si falta la autorización. Antes de habilitarlo, revisa la cola del
demo para no reenviar operaciones de prueba por accidente.

Los destinatarios se obtienen del cliente, independientemente de si tienen Gmail,
Hotmail u otro proveedor. Prueba inicialmente con un correo propio: registra una
operación y aprueba su pago para comprobar el PDF. La aceptación por SMTP no
garantiza que el mensaje llegue a la bandeja principal.

## Estado y alcance

La autenticación y renovación se comprueban con un servidor HTTP simulado en las
pruebas. El envío real no queda verificado hasta registrar la aplicación,
autorizarla y recibir un mensaje en un correo real. Esta configuración habilita
el envío en el demo de una laptop; no despliega el sitio en un servicio de hosting.

Referencias oficiales:

- [Configuración SMTP de Outlook.com](https://support.microsoft.com/es-es/outlook/pop-imap-and-smtp-settings-for-outlook-com).
- [OAuth para SMTP y permisos necesarios](https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth).
- [Autorización por código de dispositivo](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code).
