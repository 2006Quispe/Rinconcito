-- Seed / verificación módulo reservas (ddl-auto=validate: no altera esquema).
-- Ejecutar: mysql -uroot -p rinconcito_db < src/main/resources/db/seed-reservas-demo.sql

USE rinconcito_db;

-- Evento SIN pago (confirmación directa con mesa)
INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Almuerzo familiar', 'Evento demostración sin cobro de entrada', DATE_ADD(CURDATE(), INTERVAL ((9 - DAYOFWEEK(CURDATE()) + 7) % 7) DAY),
       '12:00:00', '16:00:00', NULL, 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM eventos e
    WHERE e.titulo = 'Almuerzo familiar'
      AND e.fecha = DATE_ADD(CURDATE(), INTERVAL ((9 - DAYOFWEEK(CURDATE()) + 7) % 7) DAY)
);

-- Evento CON pago (PENDIENTE_PAGO, sin mesa)
INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Almuerzo Show', 'Evento demostración con entrada', DATE_ADD(CURDATE(), INTERVAL ((1 - DAYOFWEEK(CURDATE()) + 7) % 7) DAY),
       '12:00:00', '18:00:00', NULL, 1, 25, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM eventos e
    WHERE e.titulo = 'Almuerzo Show'
      AND e.fecha = DATE_ADD(CURDATE(), INTERVAL ((1 - DAYOFWEEK(CURDATE()) + 7) % 7) DAY)
);