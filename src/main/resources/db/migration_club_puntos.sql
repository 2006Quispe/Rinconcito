-- Ejecutar una sola vez en la base rinconcito_db.
USE rinconcito_db;

ALTER TABLE pedidos
    ADD COLUMN descuento_club INT NOT NULL DEFAULT 0 AFTER costo_delivery;

CREATE TABLE club_saldos (
    id_usuario INT NOT NULL,
    puntos INT NOT NULL DEFAULT 0,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_club_saldos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT chk_club_saldos_puntos CHECK (puntos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE club_recompensas (
    id_recompensa INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    costo_puntos INT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor INT NOT NULL,
    compra_minima INT NOT NULL DEFAULT 0,
    imagen VARCHAR(255) DEFAULT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    stock INT DEFAULT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_recompensa),
    UNIQUE KEY uq_club_recompensas_codigo (codigo),
    CONSTRAINT chk_club_recompensas_costo CHECK (costo_puntos > 0),
    CONSTRAINT chk_club_recompensas_valor CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE club_canjes (
    id_canje BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_recompensa INT NOT NULL,
    id_pedido INT DEFAULT NULL,
    codigo_cupon VARCHAR(32) NOT NULL,
    nombre_recompensa VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor INT NOT NULL,
    compra_minima INT NOT NULL DEFAULT 0,
    puntos_usados INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    fecha_canje DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en DATETIME NOT NULL,
    usado_en DATETIME DEFAULT NULL,
    PRIMARY KEY (id_canje),
    UNIQUE KEY uq_club_canjes_codigo (codigo_cupon),
    UNIQUE KEY uq_club_canjes_pedido (id_pedido),
    KEY idx_club_canjes_usuario_estado (id_usuario, estado, vence_en),
    CONSTRAINT fk_club_canjes_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_club_canjes_recompensa
        FOREIGN KEY (id_recompensa) REFERENCES club_recompensas (id_recompensa),
    CONSTRAINT fk_club_canjes_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido),
    CONSTRAINT chk_club_canjes_puntos CHECK (puntos_usados > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE club_movimientos (
    id_movimiento BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_pago INT DEFAULT NULL,
    id_canje BIGINT DEFAULT NULL,
    tipo VARCHAR(20) NOT NULL,
    puntos INT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_movimiento),
    UNIQUE KEY uq_club_movimientos_pago (id_pago),
    UNIQUE KEY uq_club_movimientos_canje (id_canje),
    KEY idx_club_movimientos_usuario_fecha (id_usuario, creado_en),
    CONSTRAINT fk_club_movimientos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_club_movimientos_pago
        FOREIGN KEY (id_pago) REFERENCES pagos (id_pago),
    CONSTRAINT fk_club_movimientos_canje
        FOREIGN KEY (id_canje) REFERENCES club_canjes (id_canje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, valor, compra_minima, activo, stock)
VALUES
    ('DESCUENTO-5', 'S/ 5 de descuento', 'Descuenta S/ 5 en un pedido online desde S/ 30.', 100, 'DESCUENTO_FIJO', 5, 30, 1, NULL),
    ('DESCUENTO-10', '10% de descuento', 'Ahorra 10% en un pedido online desde S/ 50.', 180, 'DESCUENTO_PORCENTAJE', 10, 50, 1, NULL),
    ('DESCUENTO-20', 'S/ 20 de descuento', 'Descuenta S/ 20 en un pedido online desde S/ 100.', 350, 'DESCUENTO_FIJO', 20, 100, 1, NULL),
    ('DELIVERY-GRATIS', 'Delivery gratis', 'Canjea este cupón para cubrir la tarifa de delivery de un pedido online.', 100, 'DELIVERY_GRATIS', 1, 0, 1, NULL),
    ('TAZA-LOGO', 'Taza con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 450, 'PRODUCTO', 1, 0, 1, 10),
    ('GORRA-LOGO', 'Gorra con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 700, 'PRODUCTO', 1, 0, 1, 6),
    ('POLO-LOGO', 'Polo con logo Rinconcito', 'Recompensa física para recoger en caja. Presenta tu código de canje.', 900, 'PRODUCTO', 1, 0, 1, 5);

INSERT IGNORE INTO club_saldos (id_usuario, puntos)
SELECT u.id_usuario, 0
FROM usuarios u
JOIN roles r ON r.id_rol = u.id_rol
WHERE r.nombre = 'CLIENTE';
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
