# Pedidos en línea

Antes de arrancar esta versión con una base existente, ejecuta
`src/main/resources/db/migration_pedido_seguimiento.sql` una sola vez en la
base de Rinconcito. La migración agrega dos columnas y un índice.

Para cotizar envíos, agrega a `.local/application.properties` los valores
`app.delivery.latitud` y `app.delivery.longitud` con las coordenadas del
restaurante, en grados decimales. El cálculo usa distancia en línea recta.
La cobertura se filtra por el distrito indicado y un radio máximo de 6 km.

Si el cliente abre el correo desde otro dispositivo, configura
`app.url-publica` con la URL de Rinconcito accesible desde ese dispositivo.
Para el envío con Hotmail usa la configuración descrita en
[EJECUTAR_EN_OTRA_PC.md](EJECUTAR_EN_OTRA_PC.md).
