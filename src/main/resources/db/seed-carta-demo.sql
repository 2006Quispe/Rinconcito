-- Seed OPCIONAL de productos de ejemplo para la carta.
-- Ejecutar solo si la tabla productos está vacía y quieres demo.
-- Precios en soles enteros (INT). Imagen: URL pública o path relativo /img/...
-- Categorías ya existentes: 1 Entradas … 8 Postres.
-- No altera esquema (ddl-auto=validate). No se requiere ALTER.

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 1, 'Papa a la huancaína', 'Papa sancochada con crema de ají amarillo.', 12,
       'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Papa a la huancaína');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 2, 'Pachamanca especial', 'Carnes y tubérculos al horno de tierra.', 45,
       'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pachamanca especial');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 2, 'Cuy chactado', 'Cuy crocante con papas y ensalada.', 55,
       'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=600', 0, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Cuy chactado');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 3, 'Parrilla mixta', 'Res, pollo y chorizo a la parrilla.', 48,
       'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=600', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Parrilla mixta');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 4, 'Pollo a la brasa Wallpa Waccay', '1/4 de pollo con papas y cremas.', 22,
       'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=600', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pollo a la brasa Wallpa Waccay');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 7, 'Chicha morada', 'Vaso de chicha morada natural.', 6,
       'https://images.unsplash.com/photo-1546173159-315724a31696?w=600', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Chicha morada');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, imagen, disponible, estado)
SELECT 8, 'Mazamorra morada', 'Postre tradicional con frutas.', 8,
       NULL, 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Mazamorra morada');
