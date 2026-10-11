-- Seed / alter opcional módulo pago manual de reservas (ddl-auto=validate).
-- Ejecutar: C:\xampp\mysql\bin\mysql.exe -uroot -padmin123 rinconcito_db < src/main/resources/db/seed-pago-reservas.sql
--
-- La tabla pagos ya incluye: evidencia_pago, metodo_pago, numero_operacion,
-- id_usuario_validador, fecha_validacion, observacion_validacion.
-- No se requieren ALTER si el esquema actual ya está alineado con la entidad Pago.

USE rinconcito_db;

-- Datos de prueba Yape/Plin/QR (solo si están NULL)
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
