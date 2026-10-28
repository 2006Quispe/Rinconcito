# Correo Hotmail del demo

El perfil `hotmail` usa Microsoft Graph por HTTPS con OAuth2 y el permiso delegado
`Mail.Send`. El buzon del demo rechazo SMTP con `535 5.7.139` (SMTP deshabilitado).
Se reutiliza el mensaje MIME y su PDF; no se necesita otra dependencia ni hosting en Azure.

Guia para el equipo: [Ejecutar en otra PC](EJECUTAR_EN_OTRA_PC.md).

## Configuracion en cada laptop

1. En Microsoft Entra, registra `Rinconcito Demo` para cuentas personales de Microsoft.
   Se requiere acceso a un directorio y permiso para registrar aplicaciones.
2. En Autenticacion habilita los flujos de clientes publicos.
3. Ejecuta desde la carpeta del proyecto:

```powershell
python tools/autorizar_outlook.py --client-id TU_ID_PUBLICO --cuenta rinconcito.demo@hotmail.com
```

Abre la direccion que devuelve Microsoft (para esta cuenta, `https://www.microsoft.com/link`),
introduce el codigo y acepta el permiso de enviar correos de Rinconcito Demo.
El programa solicita `https://graph.microsoft.com/Mail.Send` y `offline_access`.
No hace falta agregar Exchange Online en el portal ni crear un secreto de cliente.
Si antes autorizaste SMTP.Send, vuelve a ejecutar el comando para consentir Mail.Send.
La contrasena se introduce solo en Microsoft.

La autorizacion se guarda en `.local/outlook-oauth.properties`, ignorado por Git.
Cada laptop debe autorizarse por separado; no subas ni compartas ese archivo.
Los tokens se renuevan automaticamente. Si Microsoft revoca el acceso, autoriza de nuevo.

4. Aplica `src/main/resources/db/migration_correo_salida.sql` a la base de demostracion.
   La base demo incluida ya contiene esta tabla.
5. Crea `.local/application.properties` con `spring.datasource.url`,
   `spring.datasource.username` y `spring.datasource.password` de tu MySQL local.
6. Deten la instancia anterior en 8081 y la copia de pruebas en 8082.
   Revisa que la cola no contenga mensajes de prueba pendientes.
7. Ejecuta:

```powershell
.\tools\Iniciar-Hotmail.ps1
```

El correo esta deshabilitado por defecto. El script lo habilita solo para ese proceso.
No usa `.local/correo.properties`, que pertenece al buzon simulado.

## Comprobacion

La confirmacion se encola al registrar el pedido o reserva. La nota con PDF se
encola al emitirla con un pago aprobado. Los destinatarios salen de los datos del cliente,
sea Gmail, Outlook u otro proveedor.
En `/caja/correos` se revisan los errores y se reintenta un envio fallido.
`ENVIADO` significa que el proveedor acepto el mensaje, no confirma su entrega a la bandeja principal.
Graph tambien guarda el mensaje aceptado en Elementos enviados del remitente.
Esta configuracion sirve para el demo en una laptop; no despliega el sitio en Internet.

Referencia: [Microsoft Graph sendMail](https://learn.microsoft.com/en-us/graph/api/user-sendmail?view=graph-rest-1.0).

El envio real se verifico recibiendo una nota con PDF en Gmail.
