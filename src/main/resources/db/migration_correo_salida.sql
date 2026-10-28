-- Ejecutar en la base de PRUEBA seleccionada. No incluye USE para evitar cambiar otra base.
CREATE TABLE IF NOT EXISTS correo_salida (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 clave VARCHAR(80) NOT NULL UNIQUE,
 tipo VARCHAR(20) NOT NULL,
 id_referencia INT NOT NULL,
 destinatario VARCHAR(254) NOT NULL,
 asunto VARCHAR(200) NOT NULL,
 cuerpo TEXT NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
 intentos INT NOT NULL DEFAULT 0,
 error VARCHAR(300) NULL,
 creado DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 enviado DATETIME NULL,
 proximo_intento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_correo_pendiente (estado,proximo_intento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
