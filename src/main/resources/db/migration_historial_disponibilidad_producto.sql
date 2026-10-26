USE rinconcito_db;

CREATE TABLE IF NOT EXISTS historial_disponibilidad_producto (
    id_historial BIGINT NOT NULL AUTO_INCREMENT,
    id_producto INT NOT NULL,
    id_usuario INT NOT NULL,
    disponible_anterior TINYINT(1) NOT NULL,
    disponible_nuevo TINYINT(1) NOT NULL,
    fecha_cambio DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id_historial),

    CONSTRAINT fk_historial_disponibilidad_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos (id_producto)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_historial_disponibilidad_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_historial_disponibilidad_fecha (fecha_cambio),
    INDEX idx_historial_disponibilidad_producto (id_producto),
    INDEX idx_historial_disponibilidad_usuario (id_usuario)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
