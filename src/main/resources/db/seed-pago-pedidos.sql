-- Seed opcional modulo pedidos delivery + pago (ddl-auto=validate: no altera esquema).
-- Ejecutar solo si faltan datos de Yape/Plin:
--   C:\xampp\mysql\bin\mysql.exe -uroot -padmin123 rinconcito_db < src/main/resources/db/seed-pago-pedidos.sql
--
-- La tabla pagos ya tiene id_pedido e id_reserva nullable.
-- pedidos / detalle_pedido / direcciones ya alineados con las entidades.
-- No se requiere ALTER.

USE rinconcito_db;

UPDATE configuracion_restaurante
SET
    numero_yape  = COALESCE(numero_yape,  '999111222'),
    titular_yape = COALESCE(titular_yape, 'Rinconcito Ayacuchano'),
    qr_yape      = COALESCE(qr_yape,      '/img/qr-yape.png'),
    numero_plin  = COALESCE(numero_plin,  '999333444'),
    titular_plin = COALESCE(titular_plin, 'Rinconcito Ayacuchano'),
    qr_plin      = COALESCE(qr_plin,      '/img/qr-plin.png'),
    actualizado_en = NOW()
WHERE id_configuracion = 1
  AND (numero_yape IS NULL OR numero_plin IS NULL OR qr_yape IS NULL OR qr_plin IS NULL);