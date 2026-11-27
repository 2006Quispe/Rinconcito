-- Actualiza las tablas Club creadas por migration_club_puntos.sql
-- para que coincidan con ClubPuntosService. Ejecutar una sola vez.
-- No elimina tablas ni datos existentes.
USE rinconcito_db;

ALTER TABLE club_saldos
    ADD COLUMN puntos_deuda INT NOT NULL DEFAULT 0 AFTER puntos;

ALTER TABLE club_recompensas
    ADD COLUMN categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS' AFTER imagen,
    ADD COLUMN condiciones VARCHAR(500) DEFAULT NULL AFTER categoria,
    ADD COLUMN vigencia_dias INT NOT NULL DEFAULT 90 AFTER condiciones,
    ADD COLUMN id_producto INT DEFAULT NULL AFTER vigencia_dias,
    ADD COLUMN disponible_desde DATETIME DEFAULT NULL AFTER id_producto,
    ADD COLUMN disponible_hasta DATETIME DEFAULT NULL AFTER disponible_desde,
    ADD CONSTRAINT fk_club_recompensas_producto
        FOREIGN KEY (id_producto) REFERENCES productos (id_producto) ON DELETE SET NULL;

ALTER TABLE club_canjes
    ADD COLUMN condiciones VARCHAR(500) DEFAULT NULL AFTER puntos_usados,
    ADD COLUMN categoria VARCHAR(40) NOT NULL DEFAULT 'DESCUENTOS' AFTER condiciones,
    ADD COLUMN id_usuario_validador INT DEFAULT NULL AFTER categoria,
    ADD CONSTRAINT fk_club_canjes_validador
        FOREIGN KEY (id_usuario_validador) REFERENCES usuarios (id_usuario);

CREATE TABLE club_lotes_puntos (
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
    CONSTRAINT fk_club_lotes_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_club_lotes_pago
        FOREIGN KEY (id_pago) REFERENCES pagos (id_pago),
    CONSTRAINT chk_club_lotes_originales CHECK (puntos_originales > 0),
    CONSTRAINT chk_club_lotes_disponibles
        CHECK (puntos_disponibles >= 0 AND puntos_disponibles <= puntos_originales)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE club_consumos_lotes (
    id_canje BIGINT NOT NULL,
    id_lote BIGINT NOT NULL,
    puntos INT NOT NULL,
    PRIMARY KEY (id_canje, id_lote),
    CONSTRAINT fk_club_consumos_canje
        FOREIGN KEY (id_canje) REFERENCES club_canjes (id_canje),
    CONSTRAINT fk_club_consumos_lote
        FOREIGN KEY (id_lote) REFERENCES club_lotes_puntos (id_lote),
    CONSTRAINT chk_club_consumos_puntos CHECK (puntos > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE club_movimientos
    ADD COLUMN id_lote BIGINT DEFAULT NULL AFTER id_canje,
    ADD COLUMN id_admin INT DEFAULT NULL AFTER id_lote,
    ADD COLUMN referencia_operacion VARCHAR(100) DEFAULT NULL AFTER id_admin,
    ADD COLUMN vence_en DATETIME DEFAULT NULL AFTER referencia_operacion,
    ADD UNIQUE KEY uq_club_movimientos_referencia (referencia_operacion),
    ADD CONSTRAINT fk_club_movimientos_lote
        FOREIGN KEY (id_lote) REFERENCES club_lotes_puntos (id_lote),
    ADD CONSTRAINT fk_club_movimientos_admin
        FOREIGN KEY (id_admin) REFERENCES usuarios (id_usuario);

CREATE TABLE club_configuracion (
    clave VARCHAR(50) NOT NULL,
    valor VARCHAR(100) NOT NULL,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_admin INT DEFAULT NULL,
    PRIMARY KEY (clave),
    CONSTRAINT fk_club_config_admin
        FOREIGN KEY (id_admin) REFERENCES usuarios (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE club_campanas (
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
    CONSTRAINT fk_club_campanas_admin
        FOREIGN KEY (id_admin) REFERENCES usuarios (id_usuario),
    CONSTRAINT chk_club_campanas_porcentaje CHECK (porcentaje_bonus BETWEEN 1 AND 500),
    CONSTRAINT chk_club_campanas_fechas CHECK (fecha_fin >= fecha_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Conserva como lote inicial los saldos que ya existian.
INSERT IGNORE INTO club_lotes_puntos
    (id_usuario, codigo_origen, puntos_originales, puntos_disponibles, creado_en)
SELECT id_usuario, CONCAT('MIGRACION-SALDO-', id_usuario), puntos, puntos, NOW()
FROM club_saldos
WHERE puntos > 0;

INSERT IGNORE INTO club_configuracion (clave, valor) VALUES
    ('PUNTOS_POR_SOL', '1'),
    ('VIGENCIA_PUNTOS_DIAS', '365'),
    ('VIGENCIA_RECOMPENSAS_DIAS', '90');
