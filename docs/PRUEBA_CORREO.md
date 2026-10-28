# Prueba local de correos

La funcionalidad se desarrolló y probó en `rinconcito-correo-local`, rama
`prueba/correo-local`. La integración para el demo se prepara en la rama local
`correo/hotmail-demo`, sin publicar en GitHub. Para el remitente real consulta
[CORREO_HOTMAIL.md](CORREO_HOTMAIL.md).

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

## Correo real

El remitente del equipo usa Microsoft Graph. Sigue [CORREO_HOTMAIL.md](CORREO_HOTMAIL.md)
y la [guia para otra PC](EJECUTAR_EN_OTRA_PC.md). No uses la configuracion del buzon
simulado para enviar mensajes reales. Maven descarga spring-boot-starter-mail;
H2 solo se usa en pruebas.

## Validación

Se verificaron por HTTP las confirmaciones y PDF de pedidos y reservas con el
buzon simulado, incluyendo fallo y reintento. La integracion Graph tiene pruebas
de envio MIME con PDF, errores de autorizacion y renovacion de tokens.

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
