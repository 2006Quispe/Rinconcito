-- Próximos eventos cargados desde los afiches (img/eventos).
-- Idempotente: no duplica si ya existen (mismo título y fecha). ddl-auto=validate: no altera esquema.
-- Ejecutar: mysql -uroot -p rinconcito_db < src/main/resources/db/seed-eventos-proximos.sql

USE rinconcito_db;
SET NAMES utf8mb4;

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Dina Paucar', 'Dina Paucar, Angélica María y Chinita Oshiro. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-12-13', '11:00:00', NULL, '/img/eventos/show-dina-paucar-2026-12-13.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Dina Paucar' AND e.fecha = '2026-12-13');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Jade Perú y Hnos. Ayvar', 'Jade Perú, Hnos. Ayvar y Dianita del Perú. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-12-20', '11:00:00', NULL, '/img/eventos/show-jade-peru-hnos-ayvar-2026-12-20.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Jade Perú y Hnos. Ayvar' AND e.fecha = '2026-12-20');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Navidad con Alan Vásquez', 'Celebra la Navidad con Alan Vásquez, Angélica María y Liz Campos. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-12-25', '11:00:00', NULL, '/img/eventos/navidad-alan-vasquez-2026-12-25.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Navidad con Alan Vásquez' AND e.fecha = '2026-12-25');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Ayrampito, el Rey de los Carnavales', 'Ayrampito, María López y Johanna del Mantaro. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-12-27', '11:00:00', NULL, '/img/eventos/show-ayrampito-2026-12-27.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Ayrampito, el Rey de los Carnavales' AND e.fecha = '2026-12-27');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Año 2027', 'Recibe el 2027 con Jade Perú, Alan Vásquez, Madeleine Quinto, Diamante Andino y Mario Gómez. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2027-01-01', '11:00:00', NULL, '/img/eventos/feliz-ano-2027.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Año 2027' AND e.fecha = '2027-01-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2027', 'Almuerzo show a las 11 a.m. con Diosdado Gaitán Castro, Los Runas del Perú, Dúo Ayacucho, Celaje de Andahuaylas, K''prichos, Ayrampito y Entre Manos. Animación: Wallpa Waccay. En el local más carnavalero.', '2027-02-07', '11:00:00', NULL, '/img/eventos/carnaval-chuta-chutay-2027-02-07.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2027' AND e.fecha = '2027-02-07');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Homenaje a Mamá', 'Celebra el Día de la Madre con Max Castro, Surandino, Flor Sinqueña, Alan Vásquez, Diamante Andino y Mario Gómez. Animación: Wallpa Waccay. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2027-05-09', '11:00:00', NULL, '/img/eventos/homenaje-a-mama-2027-05-09.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Homenaje a Mamá' AND e.fecha = '2027-05-09');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Día del Padre', 'Festeja a papá con Diosdado Gaitán Castro, Janeth Salinas, Los Runas del Perú, Ruby Palomino, Percy Arana y Orquesta, Huacchas del Perú, Diamante Andino y Mario Gómez. Animación: Wallpa Waccay.', '2027-06-20', '11:00:00', NULL, '/img/eventos/dia-del-padre-2027-06-20.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Día del Padre' AND e.fecha = '2027-06-20');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Fiestas Patrias', 'Celebra las Fiestas Patrias con Ayrampito, Madeleine Quinto, Pío Quispe, Mario Gómez, Percy Arana y Orquesta y Joel Eusebio. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2027-07-28', '11:00:00', NULL, '/img/eventos/fiestas-patrias-2027-07-28.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Fiestas Patrias' AND e.fecha = '2027-07-28');
