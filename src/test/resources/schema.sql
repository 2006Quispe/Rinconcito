CREATE TABLE IF NOT EXISTS club_saldos (
    id_usuario INT PRIMARY KEY,
    puntos INT NOT NULL DEFAULT 0,
    puntos_deuda INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS club_recompensas (
    id_recompensa INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    costo_puntos INT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor INT NOT NULL,
    compra_minima INT NOT NULL DEFAULT 0,
    imagen VARCHAR(255),
    categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS',
    condiciones VARCHAR(500),
    vigencia_dias INT NOT NULL DEFAULT 90,
    id_producto INT,
    disponible_desde TIMESTAMP,
    disponible_hasta TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    stock INT
);

CREATE TABLE IF NOT EXISTS club_canjes (
    id_canje BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_recompensa INT NOT NULL,
    id_pedido INT UNIQUE,
    codigo_cupon VARCHAR(32) NOT NULL UNIQUE,
    nombre_recompensa VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor INT NOT NULL,
    compra_minima INT NOT NULL DEFAULT 0,
    puntos_usados INT NOT NULL,
    condiciones VARCHAR(500),
    categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS',
    id_usuario_validador INT,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    fecha_canje TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_lotes_puntos (
    id_lote BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_pago INT UNIQUE,
    codigo_origen VARCHAR(100) NOT NULL UNIQUE,
    puntos_originales INT NOT NULL,
    puntos_disponibles INT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_consumos_lotes (
    id_canje BIGINT NOT NULL,
    id_lote BIGINT NOT NULL,
    puntos INT NOT NULL,
    PRIMARY KEY (id_canje, id_lote)
);

CREATE TABLE IF NOT EXISTS club_configuracion (
    clave VARCHAR(50) PRIMARY KEY,
    valor VARCHAR(100) NOT NULL,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_admin INT
);

CREATE TABLE IF NOT EXISTS club_campanas (
    id_campana BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    porcentaje_bonus INT NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    id_admin INT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_movimientos (
    id_movimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_pago INT UNIQUE,
    id_canje BIGINT UNIQUE,
    id_lote BIGINT,
    id_admin INT,
    referencia_operacion VARCHAR(100) UNIQUE,
    vence_en TIMESTAMP,
    tipo VARCHAR(20) NOT NULL,
    puntos INT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_pago_resumen (
    id_pago INT PRIMARY KEY,
    id_usuario INT NOT NULL,
    nivel_codigo VARCHAR(20) NOT NULL,
    nivel_nombre VARCHAR(80) NOT NULL,
    puntos_anuales_antes INT NOT NULL DEFAULT 0,
    saldo_anterior INT NOT NULL DEFAULT 0,
    deuda_anterior INT NOT NULL DEFAULT 0,
    puntos_base INT NOT NULL DEFAULT 0,
    puntos_bonus_nivel INT NOT NULL DEFAULT 0,
    puntos_bonus_campana INT NOT NULL DEFAULT 0,
    campana_nombre VARCHAR(100),
    puntos_totales INT NOT NULL DEFAULT 0,
    puntos_aplicados_deuda INT NOT NULL DEFAULT 0,
    puntos_acreditados INT NOT NULL DEFAULT 0,
    saldo_nuevo INT NOT NULL DEFAULT 0,
    deuda_nueva INT NOT NULL DEFAULT 0,
    nuevo_nivel_codigo VARCHAR(20) NOT NULL,
    nuevo_nivel_nombre VARCHAR(80) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
