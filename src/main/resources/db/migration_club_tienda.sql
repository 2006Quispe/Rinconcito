-- Actualiza una base que ya ejecutó migration_club_puntos.sql.
-- Es incremental e idempotente; no reemplaza la base ni borra datos.
USE rinconcito_db;

INSERT INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, valor, compra_minima, activo, stock)
VALUES
    ('DELIVERY-GRATIS', 'Delivery gratis', 'Canjea este cupón para cubrir la tarifa de delivery de un pedido online.', 100, 'DELIVERY_GRATIS', 1, 0, 1, NULL),
    ('TAZA-LOGO', 'Taza con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 450, 'PRODUCTO', 1, 0, 1, 10),
    ('GORRA-LOGO', 'Gorra con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 700, 'PRODUCTO', 1, 0, 1, 6),
    ('POLO-LOGO', 'Polo con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 900, 'PRODUCTO', 1, 0, 1, 5)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    descripcion = VALUES(descripcion),
    costo_puntos = VALUES(costo_puntos),
    tipo = VALUES(tipo),
    valor = VALUES(valor),
    compra_minima = VALUES(compra_minima),
    stock = COALESCE(club_recompensas.stock, VALUES(stock)),
    activo = VALUES(activo);
-- RUTAS_IMAGENES_CLUB_RECOMPENSAS
UPDATE club_recompensas
SET imagen = CASE codigo
    WHEN 'CHICHA-MORADA' THEN '/img/club/chicha-morada.webp'
    WHEN 'MAZAMORRA-MORADA' THEN '/img/club/mazamorra-morada.webp'
    WHEN 'OCOPA-AYACUCHANA' THEN '/img/club/ocopa-ayacuchana.webp'
    WHEN 'DELIVERY-GRATIS' THEN '/img/club/delivery-gratis.webp'
    WHEN 'DESCUENTO-5' THEN '/img/club/descuento-5.webp'
    WHEN 'DESCUENTO-10' THEN '/img/club/descuento-10.webp'
    WHEN 'DESCUENTO-20' THEN '/img/club/descuento-20.webp'
    WHEN 'TAZA-LOGO' THEN '/img/club/taza-logo.webp'
    WHEN 'GORRA-LOGO' THEN '/img/club/gorra-logo.webp'
    WHEN 'POLO-LOGO' THEN '/img/club/polo-logo.webp'
    ELSE imagen
END
WHERE codigo IN (
    'CHICHA-MORADA', 'MAZAMORRA-MORADA', 'OCOPA-AYACUCHANA',
    'DELIVERY-GRATIS', 'DESCUENTO-5', 'DESCUENTO-10', 'DESCUENTO-20',
    'TAZA-LOGO', 'GORRA-LOGO', 'POLO-LOGO'
);
