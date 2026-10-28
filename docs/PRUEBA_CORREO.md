# Prueba local de correos

Esta copia está en `rinconcito-correo-local`, rama `prueba/correo-local`.
No está integrada en `C:\Users\jeand\Rinconcito` ni publicada en GitHub.

## Abrir la prueba

- Aplicación: **http://localhost:8082**.
- Buzón de pruebas: **http://localhost:8025**. Todos los mensajes se quedan en el equipo.
- Caja: entrar en `/personal/login` con `caja@rinconcito.pe` / `Prueba123!`.
- Estado de envíos: **http://localhost:8082/caja/correos**, después de entrar como Caja.

Se usa `rinconcito_demo_mozo`, conservando los datos anteriores. Se añadieron las
tablas de historial de disponibilidad (necesaria para el código actualizado) y
`correo_salida`, además de las operaciones ficticias de la prueba. La base
`rinconcito_db` y el código del proyecto original no se modificaron.

## Qué probar

1. Abre la aplicación de prueba y añade productos a la carta/carrito.
2. Registra un pedido. Como invitado, escribe un correo de ejemplo, por ejemplo
   `cliente@example.test`. Con una cuenta de cliente, se usa el correo registrado.
3. Revisa el buzón: aparecerán código, productos, total y estado pendiente de pago.
4. Registra una reserva con correo. El mensaje conserva su estado real: una reserva
   pendiente no se presenta como pagada ni confirmada.
5. Para el PDF, adjunta una evidencia de prueba al pedido y apruébala entrando como
   Caja. La nota de venta generada se adjunta al correo de pago confirmado.
6. En Caja → Notas de venta, **Enviar correo** permite solicitar un envío o reenvío.
   La pantalla de correos muestra el estado; un intento fallido se puede reintentar.

En el buzón puedes pausar la recepción SMTP para simular un fallo. El pedido y su
pago permanecen guardados; reanuda la recepción y reintenta desde Caja.

Los pedidos presenciales también se notifican si el cliente asociado tiene un
correo utilizable. El consumidor final y los correos internos de invitados se
registran como `OMITIDO`. No es obligatorio tener correo para comprar o reservar.

## Cómo funciona

`ColaCorreoService` guarda una fila en `correo_salida` dentro de la transacción de
negocio. Un rollback también deshace esa fila. `ProcesadorCorreoService` consulta
la cola desde otra conexión cada cinco segundos, por lo que solo ve operaciones
confirmadas; realiza el envío fuera de la solicitud del cliente.

La clave única por operación evita duplicar la programación automática. Se hacen
hasta tres intentos, con un minuto entre errores. Un reenvío manual es intencional
y reinicia los intentos de la misma fila. `ENVIADO` significa que SMTP aceptó el
mensaje, no que fue leído o que se garantice su entrega final.

El envío automático de una nota conserva el destinatario del registro de su
operación. Al reenviar manualmente se consulta el correo actual del cliente.
Se genera el PDF con el servicio existente y se comprueba que la nota siga emitida
y su pago aprobado antes de enviarla.

La cola es una implementación pequeña para esta aplicación. No ofrece entrega
exactamente una vez: si SMTP acepta un mensaje y el proceso falla antes de guardar
el resultado, un reintento podría entregarlo otra vez. Antes de uso real conviene
revisar reenvíos y el tratamiento de fallos durante un reinicio.

## Reiniciar en este equipo

La configuración privada está en `.local/correo.properties`, ignorada por Git.
Con Java y Maven disponibles, desde esta carpeta ejecuta en dos terminales:

```powershell
python tools/buzon_local.py
```

```powershell
.\mvnw.cmd "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./.local/correo.properties" spring-boot:run
```

No ejecutes estos comandos si las mismas instancias ya están escuchando en sus
puertos. Los mensajes capturados se guardan en `.local/correos/*.eml` y el PDF
verificado en `.local/nota-ejemplo.pdf`; ninguno se sube a Git.

## Para correo real después

El cliente es el **destinatario**. El restaurante necesita además una cuenta
**remitente** con acceso SMTP. La dependencia `spring-boot-starter-mail` ya está
añadida al `pom.xml`; Maven la descarga al compilar. H2 se utiliza solo en pruebas.

El archivo de prueba fija SMTP en `127.0.0.1:1025`. Para un proveedor real se usará
otra configuración local con `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`,
`SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_AUTH` y `SMTP_STARTTLS`. No basta con cambiar el
correo del cliente ni con dejar las propiedades fijas del buzón de pruebas.

No pongas credenciales en el código o en un commit. Si se elige Gmail mediante
contraseña de aplicación, la cuenta debe tener verificación en dos pasos y esa
opción debe estar disponible; no se usa la contraseña normal. Referencia:
[Ayuda de Google](https://support.google.com/accounts/answer/185833?hl=es).

Antes de integrar, aplicar `migration_correo_salida.sql` a la base correspondiente
y habilitar `CORREO_HABILITADO=true` solo cuando esté configurado el remitente.

## Validación

Resultado: **57 pruebas aprobadas**, sin fallos ni errores. Tambi?n se verificaron
por HTTP los correos y PDF de un pedido y una reserva pagada, adem?s del fallo
y reenv?o de SMTP.

Las pruebas unitarias comprueban rollback, deduplicación, contactos inválidos,
resumen de pedido, notas sin pago aprobado, reintentos y reenvíos. Las regresiones
de usuarios, validaciones de registro, reportes, seguridad web y logging se
ejecutan junto con ellas.

`tools/probar_correo_local.py` crea operaciones ficticias en la demo, registra una
reserva, aprueba un pago desde Caja, comprueba el PDF recibido y simula fallo/reenvío
de SMTP. No envía mensajes fuera del equipo. Se puede ejecutar de nuevo mientras
ambos servidores estén activos; cada ejecución crea otros datos de prueba.

```powershell
.\mvnw.cmd "-Dtest=ColaCorreoServiceTest,AdminReporteControllerTest,ExportacionReporteServiceTest,ExportadoresReporteTest,AdminReporteServiceTest,UsuarioServiceTest,RegistroFormDTOValidationTest,LogbackConfiguracionTest" test
```
