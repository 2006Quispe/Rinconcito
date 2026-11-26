-- Eventos del restaurante cargados desde los afiches (img/eventos).
-- Idempotente: no duplica si ya existen (mismo título y fecha). ddl-auto=validate: no altera esquema.
-- Ejecutar: mysql -uroot -p rinconcito_db < src/main/resources/db/seed-eventos-flyers.sql

USE rinconcito_db;
SET NAMES utf8mb4;

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Año 2026', 'Celebra el Año Nuevo con Jade Perú, Alan Vásquez, Madeleine Quinto, Diamante Andino y Mario Gómez. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-01-01', '11:00:00', NULL, '/img/eventos/evento-2026-01-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Año 2026' AND e.fecha = '2026-01-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2026', 'Almuerzo show con Diosdado Gaitán Castro, Los Runas y más artistas en el local más carnavalero. Animación: Wallpa Waccay.', '2026-02-01', '11:00:00', NULL, '/img/eventos/evento-2026-02-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2026' AND e.fecha = '2026-02-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT '33 Aniversario de Canto Andino', 'Gran celebración con Surandino, Max Castro, Porfirio Ayvar y muchos artistas más. Venta de entradas por VAOPE.', '2026-04-19', '11:00:00', NULL, '/img/eventos/evento-2026-04-19.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = '33 Aniversario de Canto Andino' AND e.fecha = '2026-04-19');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Día del Trabajo', 'Show en vivo con Aycampito, Madeleine Quinto, Pío Quispe y Mario Gómez. Animación: Wallpa Waccay. Entrada libre.', '2026-05-01', '11:00:00', NULL, '/img/eventos/evento-2026-05-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Día del Trabajo' AND e.fecha = '2026-05-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Homenaje a Mamá', 'Celebra el Día de la Madre con Max Castro, Surandino, Diamante Andino, Mario Gómez y Alan Vásquez. Animación: Wallpa Waccay. Entrada libre.', '2026-05-10', '11:00:00', NULL, '/img/eventos/evento-2026-05-10.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Homenaje a Mamá' AND e.fecha = '2026-05-10');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Alan Vásquez y Chinita Oshiro', 'Domingo de música con Alan Vásquez, Chinita Oshiro y Madeleine Quinto. Animación: Wallpa Waccay. Entrada libre.', '2026-05-31', '11:00:00', NULL, '/img/eventos/evento-2026-05-31.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Alan Vásquez y Chinita Oshiro' AND e.fecha = '2026-05-31');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Día del Padre', 'Festejamos a papá con un almuerzo show lleno de música y sabor ayacuchano. Animación: Wallpa Waccay.', '2026-06-21', '11:00:00', NULL, '/img/eventos/evento-2026-06-21.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Día del Padre' AND e.fecha = '2026-06-21');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show en vivo con Kenny Hidalgo', 'El fenomenal Kenny Hidalgo, Liz Campos, Madeleine Quinto, Mario Gómez y Diamante Andino. Animación: Wallpa Waccay. Entrada libre.', '2026-08-02', '11:00:00', NULL, '/img/eventos/evento-2026-08-02.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show en vivo con Kenny Hidalgo' AND e.fecha = '2026-08-02');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show en vivo con Alan Vásquez', 'Alan Vásquez, Angélica María y Liz Campos en un domingo de música en vivo. Animación: Wallpa Waccay. Entrada libre.', '2026-10-18', '11:00:00', NULL, '/img/eventos/evento-2026-10-18.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show en vivo con Alan Vásquez' AND e.fecha = '2026-10-18');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT '38 Aniversario de El Rinconcito Ayacuchano', 'Celebramos 38 años con Surandino, Dúo Ayacucho, Los Runas, Marifor Gómez, Super Brillantes y Huacchas. Animación: Wallpa Waccay. Venta de entradas por VAOPE.', '2026-10-25', '13:00:00', NULL, '/img/eventos/evento-2026-10-25.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = '38 Aniversario de El Rinconcito Ayacuchano' AND e.fecha = '2026-10-25');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Dina Paucar', 'Dina Paucar, Angélica María y Chinita Oshiro. Animación: Wallpa Waccay. Entrada libre.', '2026-11-08', '11:00:00', NULL, '/img/eventos/evento-2026-11-08.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Dina Paucar' AND e.fecha = '2026-11-08');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Jade Perú y Hnos. Ayvar', 'Jade Perú, Hnos. Ayvar y Dianita del Perú. Animación: Wallpa Waccay. Entrada libre.', '2026-11-15', '11:00:00', NULL, '/img/eventos/evento-2026-11-15.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Jade Perú y Hnos. Ayvar' AND e.fecha = '2026-11-15');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Ayrampito, el Rey de los Carnavales', 'Ayrampito y María López en un gran show de domingo. Animación: Wallpa Waccay. Entrada libre.', '2026-11-22', '11:00:00', NULL, '/img/eventos/evento-2026-11-22.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Ayrampito, el Rey de los Carnavales' AND e.fecha = '2026-11-22');
