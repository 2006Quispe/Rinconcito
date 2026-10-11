-- Seed OPCIONAL para footer publico (ddl-auto=validate: no altera esquema).
-- Ejecutar manualmente en MySQL si faltan datos, por ejemplo:
--   mysql -uroot -p rinconcito_db < src/main/resources/db/seed_config_horarios.sql
--
-- Horarios esperados:
--   Lunes cerrado
--   Martes a sabado: 11:00 a. m. a 6:00 p. m.
--   Domingo: 11:00 a. m. a 10:00 p. m.

USE rinconcito_db;

-- Horarios (idempotente via UNIQUE dia_semana)
INSERT INTO horarios_atencion (dia_semana, hora_apertura, hora_cierre, abierto) VALUES
    ('LUNES',     NULL,     NULL,     0),
    ('MARTES',    '11:00:00', '18:00:00', 1),
    ('MIERCOLES', '11:00:00', '18:00:00', 1),
    ('JUEVES',    '11:00:00', '18:00:00', 1),
    ('VIERNES',   '11:00:00', '18:00:00', 1),
    ('SABADO',    '11:00:00', '18:00:00', 1),
    ('DOMINGO',   '11:00:00', '22:00:00', 1)
ON DUPLICATE KEY UPDATE
    hora_apertura = VALUES(hora_apertura),
    hora_cierre   = VALUES(hora_cierre),
    abierto       = VALUES(abierto);

-- Configuracion de contacto / redes (descomentar y completar valores reales)
-- UPDATE configuracion_restaurante
-- SET nombre          = 'Rinconcito Ayacuchano Campestre',
--     direccion       = 'Carapongo, Lurigancho-Chosica, Lima',
--     referencia      = 'Referencia visible para clientes',
--     telefono        = '01XXXXXXX',
--     whatsapp        = '9XXXXXXXX',
--     correo          = 'contacto@ejemplo.com',
--     url_google_maps = 'https://maps.google.com/?q=Carapongo',
--     url_facebook    = 'https://facebook.com/tu-pagina',
--     url_instagram   = 'https://instagram.com/tu-cuenta',
--     actualizado_en  = NOW()
-- WHERE id_configuracion = 1;