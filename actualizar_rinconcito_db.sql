-- Actualiza la base existente rinconcito_db para el Club de puntos.
-- Se puede ejecutar más de una vez: conserva datos y omite columnas,
-- tablas y recompensas que ya existan.
USE rinconcito_db;

SET @club_descuento_existe = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'descuento_club'
);
SET @club_alter_pedidos = IF(
    @club_descuento_existe = 0,
    'ALTER TABLE pedidos ADD COLUMN descuento_club INT NOT NULL DEFAULT 0 AFTER costo_delivery',
    'SELECT ''La columna pedidos.descuento_club ya existe'' AS mensaje'
);
PREPARE club_stmt FROM @club_alter_pedidos;
EXECUTE club_stmt;
DEALLOCATE PREPARE club_stmt;

CREATE TABLE IF NOT EXISTS club_saldos (
    id_usuario INT NOT NULL,
    puntos INT NOT NULL DEFAULT 0,
    puntos_deuda INT NOT NULL DEFAULT 0,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_club_saldos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT chk_club_saldos_puntos CHECK (puntos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS club_recompensas (
    id_recompensa INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    costo_puntos INT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor INT NOT NULL,
    compra_minima INT NOT NULL DEFAULT 0,
    imagen VARCHAR(255) DEFAULT NULL,
    categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS',
    condiciones VARCHAR(500) DEFAULT NULL,
    vigencia_dias INT NOT NULL DEFAULT 90,
    id_producto INT DEFAULT NULL,
    disponible_desde DATETIME DEFAULT NULL,
    disponible_hasta DATETIME DEFAULT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    stock INT DEFAULT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_recompensa),
    UNIQUE KEY uq_club_recompensas_codigo (codigo),
    CONSTRAINT chk_club_recompensas_costo CHECK (costo_puntos > 0),
    CONSTRAINT chk_club_recompensas_valor CHECK (valor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS club_canjes (
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
    condiciones VARCHAR(500) DEFAULT NULL,
    categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS',
    id_usuario_validador INT DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS club_movimientos (
    id_movimiento BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_pago INT DEFAULT NULL,
    id_canje BIGINT DEFAULT NULL,
    id_lote BIGINT DEFAULT NULL,
    id_admin INT DEFAULT NULL,
    referencia_operacion VARCHAR(100) DEFAULT NULL,
    vence_en DATETIME DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS club_pago_resumen (
    id_pago INT NOT NULL,
    id_usuario INT NOT NULL,
    nivel_codigo VARCHAR(20) NOT NULL,
    nivel_nombre VARCHAR(80) NOT NULL,
    puntos_anuales_antes INT NOT NULL DEFAULT 0,
    saldo_anterior INT NOT NULL DEFAULT 0,
    deuda_anterior INT NOT NULL DEFAULT 0,
    puntos_base INT NOT NULL DEFAULT 0,
    puntos_bonus_nivel INT NOT NULL DEFAULT 0,
    puntos_bonus_campana INT NOT NULL DEFAULT 0,
    campana_nombre VARCHAR(100) DEFAULT NULL,
    puntos_totales INT NOT NULL DEFAULT 0,
    puntos_aplicados_deuda INT NOT NULL DEFAULT 0,
    puntos_acreditados INT NOT NULL DEFAULT 0,
    saldo_nuevo INT NOT NULL DEFAULT 0,
    deuda_nueva INT NOT NULL DEFAULT 0,
    nuevo_nivel_codigo VARCHAR(20) NOT NULL,
    nuevo_nivel_nombre VARCHAR(80) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_pago),
    KEY idx_club_pago_resumen_usuario (id_usuario, creado_en),
    CONSTRAINT fk_club_pago_resumen_pago
        FOREIGN KEY (id_pago) REFERENCES pagos (id_pago),
    CONSTRAINT fk_club_pago_resumen_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
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

-- Amplía el catálogo sin recrear tablas ni borrar registros.
SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_saldos' AND column_name = 'puntos_deuda');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_saldos ADD COLUMN puntos_deuda INT NOT NULL DEFAULT 0 AFTER puntos', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'categoria');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN categoria VARCHAR(40) NOT NULL DEFAULT ''DESCUENTOS''', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'condiciones');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN condiciones VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'vigencia_dias');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN vigencia_dias INT NOT NULL DEFAULT 90', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'id_producto');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN id_producto INT DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'disponible_desde');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN disponible_desde DATETIME DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_recompensas' AND column_name = 'disponible_hasta');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD COLUMN disponible_hasta DATETIME DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_canjes' AND column_name = 'condiciones');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_canjes ADD COLUMN condiciones VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_canjes' AND column_name = 'categoria');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_canjes ADD COLUMN categoria VARCHAR(40) NOT NULL DEFAULT ''DESCUENTOS''', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_canjes' AND column_name = 'id_usuario_validador');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_canjes ADD COLUMN id_usuario_validador INT DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_movimientos' AND column_name = 'referencia_operacion');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_movimientos ADD COLUMN referencia_operacion VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_movimientos' AND column_name = 'id_lote');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_movimientos ADD COLUMN id_lote BIGINT DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_movimientos' AND column_name = 'id_admin');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_movimientos ADD COLUMN id_admin INT DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'club_movimientos' AND column_name = 'vence_en');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_movimientos ADD COLUMN vence_en DATETIME DEFAULT NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

CREATE TABLE IF NOT EXISTS club_lotes_puntos (
    id_lote BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_pago INT DEFAULT NULL,
    codigo_origen VARCHAR(100) NOT NULL,
    puntos_originales INT NOT NULL,
    puntos_disponibles INT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en DATETIME DEFAULT NULL,
    PRIMARY KEY (id_lote),
    UNIQUE KEY uq_club_lotes_codigo_origen (codigo_origen),
    UNIQUE KEY uq_club_lotes_pago (id_pago),
    KEY idx_club_lotes_vencimiento (id_usuario, vence_en, puntos_disponibles),
    CONSTRAINT fk_club_lotes_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_club_lotes_pago FOREIGN KEY (id_pago) REFERENCES pagos (id_pago),
    CONSTRAINT chk_club_lotes_originales CHECK (puntos_originales > 0),
    CONSTRAINT chk_club_lotes_disponibles CHECK (puntos_disponibles >= 0 AND puntos_disponibles <= puntos_originales)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS club_consumos_lotes (
    id_canje BIGINT NOT NULL,
    id_lote BIGINT NOT NULL,
    puntos INT NOT NULL,
    PRIMARY KEY (id_canje, id_lote),
    CONSTRAINT fk_club_consumos_canje FOREIGN KEY (id_canje) REFERENCES club_canjes (id_canje),
    CONSTRAINT fk_club_consumos_lote FOREIGN KEY (id_lote) REFERENCES club_lotes_puntos (id_lote),
    CONSTRAINT chk_club_consumos_puntos CHECK (puntos > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS club_configuracion (
    clave VARCHAR(50) NOT NULL,
    valor VARCHAR(100) NOT NULL,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_admin INT DEFAULT NULL,
    PRIMARY KEY (clave),
    CONSTRAINT fk_club_config_admin FOREIGN KEY (id_admin) REFERENCES usuarios (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS club_campanas (
    id_campana BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    porcentaje_bonus INT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    activa TINYINT(1) NOT NULL DEFAULT 1,
    id_admin INT DEFAULT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_campana),
    KEY idx_club_campanas_vigencia (activa, fecha_inicio, fecha_fin),
    CONSTRAINT fk_club_campanas_admin FOREIGN KEY (id_admin) REFERENCES usuarios (id_usuario),
    CONSTRAINT chk_club_campanas_porcentaje CHECK (porcentaje_bonus BETWEEN 1 AND 500),
    CONSTRAINT chk_club_campanas_fechas CHECK (fecha_fin >= fecha_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'club_movimientos' AND index_name = 'uq_club_movimientos_referencia');
SET @club_referencias_duplicadas = (
    SELECT COUNT(*) FROM (
        SELECT referencia_operacion
        FROM club_movimientos
        WHERE referencia_operacion IS NOT NULL
        GROUP BY referencia_operacion
        HAVING COUNT(*) > 1
    ) AS referencias_duplicadas
);
SET @club_sql = IF(
    @club_existe = 0 AND @club_referencias_duplicadas = 0,
    'ALTER TABLE club_movimientos ADD UNIQUE KEY uq_club_movimientos_referencia (referencia_operacion)',
    'SELECT ''Se conserva el historial: revisa referencias duplicadas antes de añadir la clave única'' AS mensaje'
);
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'club_movimientos' AND constraint_name = 'fk_club_movimientos_lote');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_movimientos ADD CONSTRAINT fk_club_movimientos_lote FOREIGN KEY (id_lote) REFERENCES club_lotes_puntos (id_lote)', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'club_recompensas' AND constraint_name = 'fk_club_recompensas_producto');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_recompensas ADD CONSTRAINT fk_club_recompensas_producto FOREIGN KEY (id_producto) REFERENCES productos (id_producto) ON DELETE SET NULL', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

SET @club_existe = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'club_canjes' AND constraint_name = 'fk_club_canjes_validador');
SET @club_sql = IF(@club_existe = 0, 'ALTER TABLE club_canjes ADD CONSTRAINT fk_club_canjes_validador FOREIGN KEY (id_usuario_validador) REFERENCES usuarios (id_usuario)', 'SELECT 1');
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;

INSERT IGNORE INTO club_configuracion (clave, valor) VALUES
    ('PUNTOS_POR_SOL', '1'),
    ('VIGENCIA_PUNTOS_DIAS', '365'),
    ('VIGENCIA_RECOMPENSAS_DIAS', '90');

-- El saldo anterior se conserva como un lote sin vencimiento. La nueva vigencia
-- de 365 días solo se aplica a los puntos que se acumulen después de esta migración.
INSERT IGNORE INTO club_lotes_puntos
    (id_usuario, id_pago, codigo_origen, puntos_originales, puntos_disponibles, creado_en, vence_en)
SELECT id_usuario, NULL, CONCAT('SALDO-INICIAL-', id_usuario), puntos, puntos, NOW(), NULL
FROM club_saldos
WHERE puntos > 0;

UPDATE club_recompensas
SET tipo = 'PRODUCTO_MERCH', categoria = 'MERCHANDISING',
    condiciones = COALESCE(condiciones, 'Válido para recojo en el restaurante. Presenta tu código en caja.'),
    imagen = COALESCE(imagen, '/img/logo-rinconcito-nombre.png')
WHERE codigo IN ('TAZA-LOGO', 'GORRA-LOGO', 'POLO-LOGO')
  AND tipo = 'PRODUCTO';

SET @club_taza_migrada = (
    SELECT COUNT(*) FROM club_configuracion
    WHERE clave = 'MIGRACION_TAZA_500_APLICADA'
);
SET @club_sql = IF(
    @club_taza_migrada = 0,
    'UPDATE club_recompensas SET costo_puntos = 500 WHERE codigo = ''TAZA-LOGO'' AND costo_puntos = 450',
    'SELECT 1'
);
PREPARE club_stmt FROM @club_sql; EXECUTE club_stmt; DEALLOCATE PREPARE club_stmt;
INSERT IGNORE INTO club_configuracion (clave, valor)
VALUES ('MIGRACION_TAZA_500_APLICADA', '1');

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, imagen, activo, stock, vigencia_dias, id_producto)
SELECT 'CHICHA-MORADA', p.nombre, CONCAT('Recompensa de la carta: ', p.nombre, '.'), 120,
       'PRODUCTO_CARTA', 'BEBIDAS_POSTRES', 1, 0,
       'Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',
       p.imagen, 1, NULL, 90, p.id_producto
FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
WHERE LOWER(p.nombre) = LOWER('Chicha morada')
  AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO';

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, imagen, activo, stock, vigencia_dias, id_producto)
SELECT 'MAZAMORRA-MORADA', p.nombre, CONCAT('Recompensa de la carta: ', p.nombre, '.'), 150,
       'PRODUCTO_CARTA', 'BEBIDAS_POSTRES', 1, 0,
       'Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',
       p.imagen, 1, NULL, 90, p.id_producto
FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
WHERE LOWER(p.nombre) = LOWER('Mazamorra morada')
  AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO';

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, imagen, activo, stock, vigencia_dias, id_producto)
SELECT 'OCOPA-AYACUCHANA', p.nombre, CONCAT('Recompensa de la carta: ', p.nombre, '.'), 200,
       'PRODUCTO_CARTA', 'PLATOS_TIPICOS', 1, 0,
       'Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',
       p.imagen, 1, NULL, 90, p.id_producto
FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
WHERE LOWER(p.nombre) = LOWER('Ocopa ayacuchana')
  AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO';

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, imagen, activo, stock, vigencia_dias, id_producto)
SELECT 'PACHAMANCA-ESPECIAL', p.nombre, CONCAT('Recompensa de la carta: ', p.nombre, '.'), 450,
       'PRODUCTO_CARTA', 'PLATOS_TIPICOS', 1, 0,
       'Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta. Canje disponible únicamente de lunes a viernes.',
       p.imagen, 1, NULL, 90, p.id_producto
FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
WHERE LOWER(p.nombre) = LOWER('Pachamanca especial')
  AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO';

INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, imagen, activo, stock, vigencia_dias, id_producto)
SELECT 'CUY-CHACTADO', p.nombre, CONCAT('Recompensa de la carta: ', p.nombre, '.'), 550,
       'PRODUCTO_CARTA', 'PLATOS_TIPICOS', 1, 0,
       'Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta. Canje disponible únicamente de lunes a viernes.',
       p.imagen, 1, NULL, 90, p.id_producto
FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
WHERE LOWER(p.nombre) = LOWER('Cuy chactado')
  AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO';

UPDATE club_recompensas
SET condiciones = CONCAT(RTRIM(COALESCE(condiciones, '')), ' Canje disponible únicamente de lunes a viernes.')
WHERE codigo IN ('PACHAMANCA-ESPECIAL', 'CUY-CHACTADO')
  AND COALESCE(condiciones, '') NOT LIKE '%lunes a viernes%';

-- Estas referencias no aparecen al cliente hasta que Administración configure
-- y active su disponibilidad real.
INSERT IGNORE INTO club_recompensas
    (codigo, nombre, descripcion, costo_puntos, tipo, categoria, valor, compra_minima, condiciones, activo, stock, vigencia_dias)
VALUES
    ('FUENTE-COMPARTIR', 'Fuente para compartir', 'Recompensa pendiente de configuración de producto y disponibilidad.', 900, 'EXPERIENCIA', 'EXPERIENCIAS_FAMILIARES', 1, 0, 'Debe configurarse y activarse desde Administración.', 0, 0, 90),
    ('ALMUERZO-FAMILIAR', 'Almuerzo familiar', 'Recompensa pendiente de configuración de experiencia y disponibilidad.', 1800, 'EXPERIENCIA', 'EXPERIENCIAS_FAMILIARES', 1, 0, 'Debe configurarse y activarse desde Administración.', 0, 0, 90);
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

-- Valor de referencia para merchandising y detalle de recompensas en pedidos.
SET @club_precio_ref_existe = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'club_recompensas'
      AND column_name = 'precio_referencia'
);
SET @club_alter_precio_ref = IF(
    @club_precio_ref_existe = 0,
    'ALTER TABLE club_recompensas ADD COLUMN precio_referencia INT NULL AFTER id_producto',
    'SELECT ''club_recompensas.precio_referencia ya existe'' AS mensaje'
);
PREPARE club_stmt FROM @club_alter_precio_ref;
EXECUTE club_stmt;
DEALLOCATE PREPARE club_stmt;

ALTER TABLE detalle_pedido MODIFY COLUMN id_producto INT NULL;

SET @club_detalle_nombre_existe = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'detalle_pedido'
      AND column_name = 'nombre_producto'
);
SET @club_alter_detalle_nombre = IF(
    @club_detalle_nombre_existe = 0,
    'ALTER TABLE detalle_pedido ADD COLUMN nombre_producto VARCHAR(100) NULL AFTER subtotal',
    'SELECT ''detalle_pedido.nombre_producto ya existe'' AS mensaje'
);
PREPARE club_stmt FROM @club_alter_detalle_nombre;
EXECUTE club_stmt;
DEALLOCATE PREPARE club_stmt;

SET @club_detalle_recompensa_existe = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'detalle_pedido'
      AND column_name = 'es_recompensa_club'
);
SET @club_alter_detalle_recompensa = IF(
    @club_detalle_recompensa_existe = 0,
    'ALTER TABLE detalle_pedido ADD COLUMN es_recompensa_club TINYINT(1) NOT NULL DEFAULT 0 AFTER nombre_producto',
    'SELECT ''detalle_pedido.es_recompensa_club ya existe'' AS mensaje'
);
PREPARE club_stmt FROM @club_alter_detalle_recompensa;
EXECUTE club_stmt;
DEALLOCATE PREPARE club_stmt;

SET @club_detalle_descuento_existe = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'detalle_pedido'
      AND column_name = 'descuento_club'
);
SET @club_alter_detalle_descuento = IF(
    @club_detalle_descuento_existe = 0,
    'ALTER TABLE detalle_pedido ADD COLUMN descuento_club INT NOT NULL DEFAULT 0 AFTER es_recompensa_club',
    'SELECT ''detalle_pedido.descuento_club ya existe'' AS mensaje'
);
PREPARE club_stmt FROM @club_alter_detalle_descuento;
EXECUTE club_stmt;
DEALLOCATE PREPARE club_stmt;

UPDATE club_recompensas
SET descripcion = 'Recompensa exclusiva del Club: presencial, delivery o recojo en local.',
    condiciones = 'Disponible presencialmente, en delivery o recojo en local.'
WHERE codigo IN ('TAZA-LOGO', 'GORRA-LOGO', 'POLO-LOGO');
UPDATE club_recompensas SET precio_referencia = 25
WHERE codigo = 'TAZA-LOGO' AND precio_referencia IS NULL;
UPDATE club_recompensas SET precio_referencia = 35
WHERE codigo = 'GORRA-LOGO' AND precio_referencia IS NULL;
UPDATE club_recompensas SET precio_referencia = 45
WHERE codigo = 'POLO-LOGO' AND precio_referencia IS NULL;
