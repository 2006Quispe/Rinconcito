-- Base independiente para capturas del modulo Mozo de Rinconcito.
-- Ejecutar UNA VEZ desde una conexion con permiso CREATE DATABASE y GRANT (ej. root).
-- No elimina ni modifica rinconcito_db. Todos los usuarios/datos son ficticios.
-- Clave de las cuentas de prueba: Prueba123!
CREATE DATABASE `rinconcito_demo_mozo` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rinconcito_demo_mozo`;
SET NAMES utf8mb4;
SET SESSION time_zone = '-05:00';
SET @fk_anterior = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `categorias` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `uq_categorias_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comandas` (
  `id_comanda` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `numero_comanda` varchar(25) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('PENDIENTE','EN_PREPARACION','LISTA','ENTREGADA','CANCELADA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE',
  `observaciones` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_comanda`),
  UNIQUE KEY `uq_comandas_numero` (`numero_comanda`),
  KEY `fk_comandas_pedido` (`id_pedido`),
  CONSTRAINT `fk_comandas_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `configuracion_restaurante` (
  `id_configuracion` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `direccion` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `referencia` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `whatsapp` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_google_maps` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_facebook` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_instagram` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_yape` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_yape` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qr_yape` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_plin` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_plin` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qr_plin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `banco_transferencia` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_transferencia` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_cuenta_transferencia` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cci_transferencia` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_configuracion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `detalle_pedido` (
  `id_detalle` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `id_producto` int NOT NULL,
  `cantidad` int NOT NULL,
  `precio_unitario` int NOT NULL,
  `subtotal` int NOT NULL,
  `observaciones` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_detalle`),
  KEY `fk_detalle_pedido` (`id_pedido`),
  KEY `fk_detalle_producto` (`id_producto`),
  CONSTRAINT `fk_detalle_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  CONSTRAINT `chk_detalle_cantidad` CHECK ((`cantidad` > 0)),
  CONSTRAINT `chk_detalle_precio` CHECK (((`precio_unitario` >= 0) and (`subtotal` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `direcciones` (
  `id_direccion` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `direccion` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `referencia` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distrito` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitud` decimal(10,7) DEFAULT NULL,
  `longitud` decimal(10,7) DEFAULT NULL,
  `principal` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_direccion`),
  KEY `fk_direcciones_usuario` (`id_usuario`),
  CONSTRAINT `fk_direcciones_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `eventos` (
  `id_evento` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha` date NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time DEFAULT NULL,
  `imagen` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pago` tinyint(1) NOT NULL DEFAULT '0',
  `precio_entrada` int NOT NULL DEFAULT '0',
  `estado` enum('ACTIVO','INACTIVO','CANCELADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_evento`),
  KEY `idx_eventos_fecha_estado` (`fecha`,`estado`),
  CONSTRAINT `chk_eventos_precio` CHECK ((`precio_entrada` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `galeria` (
  `id_imagen` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ruta_imagen` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_publicacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('ACTIVO','INACTIVO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_imagen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `horarios_atencion` (
  `id_horario` int NOT NULL AUTO_INCREMENT,
  `dia_semana` enum('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `hora_apertura` time DEFAULT NULL,
  `hora_cierre` time DEFAULT NULL,
  `abierto` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_horario`),
  UNIQUE KEY `uq_horarios_dia` (`dia_semana`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mesas` (
  `id_mesa` int NOT NULL AUTO_INCREMENT,
  `id_piso` int NOT NULL,
  `numero` int NOT NULL,
  `capacidad` int NOT NULL,
  `estado` enum('DISPONIBLE','OCUPADA','RESERVADA','INACTIVA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DISPONIBLE',
  PRIMARY KEY (`id_mesa`),
  UNIQUE KEY `uq_mesas_piso_numero` (`id_piso`,`numero`),
  KEY `idx_mesas_piso_estado` (`id_piso`,`estado`),
  CONSTRAINT `fk_mesas_piso` FOREIGN KEY (`id_piso`) REFERENCES `pisos` (`id_piso`),
  CONSTRAINT `chk_mesas_capacidad` CHECK ((`capacidad` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notas_venta` (
  `id_nota` int NOT NULL AUTO_INCREMENT,
  `id_pago` int NOT NULL,
  `id_usuario_emisor` int NOT NULL,
  `serie` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NV01',
  `numero` int NOT NULL,
  `cliente_nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cliente_documento` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total` int NOT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('EMITIDA','ANULADA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMITIDA',
  `motivo_anulacion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_anulacion` datetime DEFAULT NULL,
  PRIMARY KEY (`id_nota`),
  UNIQUE KEY `uq_notas_venta_pago` (`id_pago`),
  UNIQUE KEY `uq_notas_venta_serie_numero` (`serie`,`numero`),
  KEY `fk_notas_venta_emisor` (`id_usuario_emisor`),
  CONSTRAINT `fk_notas_venta_emisor` FOREIGN KEY (`id_usuario_emisor`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_notas_venta_pago` FOREIGN KEY (`id_pago`) REFERENCES `pagos` (`id_pago`),
  CONSTRAINT `chk_notas_venta_total` CHECK ((`total` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pagos` (
  `id_pago` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int DEFAULT NULL,
  `id_reserva` int DEFAULT NULL,
  `metodo_pago` enum('YAPE','PLIN','EFECTIVO','TRANSFERENCIA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto` int NOT NULL,
  `numero_operacion` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidencia_pago` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('PENDIENTE_VALIDACION','APROBADO','RECHAZADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  `id_usuario_validador` int DEFAULT NULL,
  `fecha_pago` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_validacion` datetime DEFAULT NULL,
  `observacion_validacion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_pago`),
  UNIQUE KEY `uq_pagos_numero_operacion` (`numero_operacion`),
  KEY `fk_pagos_pedido` (`id_pedido`),
  KEY `fk_pagos_reserva` (`id_reserva`),
  KEY `fk_pagos_validador` (`id_usuario_validador`),
  KEY `idx_pagos_estado` (`estado`),
  KEY `idx_pagos_fecha` (`fecha_pago`),
  CONSTRAINT `fk_pagos_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  CONSTRAINT `fk_pagos_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`),
  CONSTRAINT `fk_pagos_validador` FOREIGN KEY (`id_usuario_validador`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_pagos_monto` CHECK ((`monto` > 0)),
  CONSTRAINT `chk_pagos_origen` CHECK ((((`id_pedido` is not null) and (`id_reserva` is null)) or ((`id_pedido` is null) and (`id_reserva` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pedidos` (
  `id_pedido` int NOT NULL AUTO_INCREMENT,
  `codigo_pedido` varchar(25) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_cliente` int NOT NULL,
  `id_mesero` int DEFAULT NULL,
  `id_mesa` int DEFAULT NULL,
  `id_direccion` int DEFAULT NULL,
  `tipo_pedido` enum('DELIVERY','PRESENCIAL','RECOJO_LOCAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('PENDIENTE_PAGO','PENDIENTE_VALIDACION','CONFIRMADO','CANCELADO','FINALIZADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE_PAGO',
  `subtotal` int NOT NULL DEFAULT '0',
  `costo_delivery` int NOT NULL DEFAULT '0',
  `total` int NOT NULL DEFAULT '0',
  `observaciones` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pedido`),
  UNIQUE KEY `uq_pedidos_codigo` (`codigo_pedido`),
  KEY `fk_pedidos_cliente` (`id_cliente`),
  KEY `fk_pedidos_mesero` (`id_mesero`),
  KEY `fk_pedidos_mesa` (`id_mesa`),
  KEY `fk_pedidos_direccion` (`id_direccion`),
  KEY `idx_pedidos_estado` (`estado`),
  KEY `idx_pedidos_fecha` (`fecha_registro`),
  CONSTRAINT `fk_pedidos_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_pedidos_direccion` FOREIGN KEY (`id_direccion`) REFERENCES `direcciones` (`id_direccion`),
  CONSTRAINT `fk_pedidos_mesa` FOREIGN KEY (`id_mesa`) REFERENCES `mesas` (`id_mesa`),
  CONSTRAINT `fk_pedidos_mesero` FOREIGN KEY (`id_mesero`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_pedidos_montos` CHECK (((`subtotal` >= 0) and (`costo_delivery` >= 0) and (`total` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pisos` (
  `id_piso` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_piso`),
  UNIQUE KEY `uq_pisos_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `productos` (
  `id_producto` int NOT NULL AUTO_INCREMENT,
  `id_categoria` int NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `precio` int NOT NULL,
  `imagen` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disponible` tinyint(1) NOT NULL DEFAULT '1',
  `estado` enum('ACTIVO','INACTIVO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_producto`),
  KEY `idx_productos_categoria` (`id_categoria`),
  CONSTRAINT `fk_productos_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`),
  CONSTRAINT `chk_productos_precio` CHECK ((`precio` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reservas` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `codigo_reserva` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_cliente` int NOT NULL,
  `id_piso_solicitado` int NOT NULL,
  `id_mesa` int DEFAULT NULL,
  `id_evento` int DEFAULT NULL,
  `fecha_reserva` date NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  `numero_personas` int NOT NULL,
  `ocasion_especial` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comentarios` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pago` tinyint(1) NOT NULL DEFAULT '0',
  `monto_reserva` int NOT NULL DEFAULT '0',
  `estado` enum('SOLICITADA','PENDIENTE_PAGO','PENDIENTE_VALIDACION','CONFIRMADA','CLIENTE_LLEGO','EN_ATENCION','FINALIZADA','CANCELADA','RECHAZADA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SOLICITADA',
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_reserva`),
  UNIQUE KEY `uq_reservas_codigo` (`codigo_reserva`),
  KEY `fk_reservas_cliente` (`id_cliente`),
  KEY `fk_reservas_evento` (`id_evento`),
  KEY `idx_reservas_fecha_horario` (`fecha_reserva`,`hora_inicio`,`hora_fin`),
  KEY `idx_reservas_piso` (`id_piso_solicitado`),
  KEY `idx_reservas_mesa_fecha` (`id_mesa`,`fecha_reserva`),
  KEY `idx_reservas_estado` (`estado`),
  CONSTRAINT `fk_reservas_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_reservas_evento` FOREIGN KEY (`id_evento`) REFERENCES `eventos` (`id_evento`),
  CONSTRAINT `fk_reservas_mesa` FOREIGN KEY (`id_mesa`) REFERENCES `mesas` (`id_mesa`),
  CONSTRAINT `fk_reservas_piso` FOREIGN KEY (`id_piso_solicitado`) REFERENCES `pisos` (`id_piso`),
  CONSTRAINT `chk_reservas_horario` CHECK ((`hora_fin` > `hora_inicio`)),
  CONSTRAINT `chk_reservas_monto` CHECK ((`monto_reserva` >= 0)),
  CONSTRAINT `chk_reservas_personas` CHECK ((`numero_personas` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles` (
  `id_rol` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `uq_roles_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `id_rol` int NOT NULL,
  `nombres` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `apellidos` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dni` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(9) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contrasena` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_registro` enum('CUENTA','INVITADO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUENTA',
  `estado` enum('ACTIVO','INACTIVO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuarios_dni` (`dni`),
  UNIQUE KEY `uq_usuarios_correo` (`correo`),
  KEY `idx_usuarios_rol` (`id_rol`),
  CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = @fk_anterior;

START TRANSACTION;
SET @ahora = NOW(), @hoy = CURDATE(), @ayer = DATE_SUB(CURDATE(), INTERVAL 1 DAY);

SET @inicio = GREATEST(TIME('00:00:00'), SUBTIME(CURTIME(), '00:15:00')), @fin = LEAST(TIME('23:59:59'), ADDTIME(CURTIME(), '01:45:00'));

INSERT INTO `roles` (`id_rol`, `nombre`) VALUES
(1, 'ADMINISTRADOR'),
(2, 'MESERO'),
(3, 'CAJA'),
(4, 'CLIENTE');

INSERT INTO `usuarios` (`id_usuario`, `id_rol`, `nombres`, `apellidos`, `dni`, `telefono`, `correo`, `contrasena`, `tipo_registro`, `estado`, `fecha_registro`) VALUES
(1, 1, 'Admin', 'Prueba', '20000001', '900000001', 'admin@rinconcito.pe', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(2, 2, 'Mateo', 'Mozo Demo', '20000002', '900000002', 'mesero@rinconcito.pe', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(3, 3, 'Caja', 'Prueba', '20000003', '900000003', 'caja@rinconcito.pe', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(4, 4, 'Lucía', 'Demo', '20000004', '900000004', 'lucia.demo@example.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(5, 4, 'Diego', 'Demo', '20000005', '900000005', 'diego.demo@example.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(6, 4, 'Valeria', 'Demo', '20000006', '900000006', 'valeria.demo@example.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(7, 4, 'Andrés', 'Demo', '20000007', '900000007', 'andres.demo@example.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora),
(8, 4, 'Camila', 'Demo', '20000008', '900000008', 'camila.demo@example.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', @ahora);

INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES
(1, 'Entradas', 'Opciones de entradas', 'ACTIVO'),
(2, 'Platos típicos', 'Opciones de platos típicos', 'ACTIVO'),
(3, 'Parrillas', 'Opciones de parrillas', 'ACTIVO'),
(4, 'Pollo a la brasa', 'Opciones de pollo a la brasa', 'ACTIVO'),
(5, 'Sopas', 'Opciones de sopas', 'ACTIVO'),
(6, 'Ensaladas', 'Opciones de ensaladas', 'ACTIVO'),
(7, 'Bebidas', 'Opciones de bebidas', 'ACTIVO'),
(8, 'Postres', 'Opciones de postres', 'ACTIVO');

INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `precio`, `imagen`, `disponible`, `estado`) VALUES
(1, 1, 'Papa a la huancaína', 'Preparación de muestra para el prototipo del restaurante.', 12, NULL, 1, 'ACTIVO'),
(2, 2, 'Pachamanca especial', 'Preparación de muestra para el prototipo del restaurante.', 45, '/img/pachamanca.png', 1, 'ACTIVO'),
(3, 2, 'Cuy chactado', 'Preparación de muestra para el prototipo del restaurante.', 55, '/img/cuy-chactado.png', 1, 'ACTIVO'),
(4, 3, 'Parrilla mixta', 'Preparación de muestra para el prototipo del restaurante.', 48, NULL, 1, 'ACTIVO'),
(5, 4, 'Pollo a la brasa - cuarto', 'Preparación de muestra para el prototipo del restaurante.', 22, '/img/pollo-brasa.png', 1, 'ACTIVO'),
(6, 7, 'Chicha morada', 'Preparación de muestra para el prototipo del restaurante.', 6, NULL, 1, 'ACTIVO'),
(7, 8, 'Mazamorra morada', 'Preparación de muestra para el prototipo del restaurante.', 8, NULL, 1, 'ACTIVO'),
(8, 1, 'Ocopa ayacuchana', 'Preparación de muestra para el prototipo del restaurante.', 12, NULL, 1, 'ACTIVO'),
(9, 2, 'Puca picante con chicharrón', 'Preparación de muestra para el prototipo del restaurante.', 30, NULL, 1, 'ACTIVO'),
(10, 2, 'Lomo saltado', 'Preparación de muestra para el prototipo del restaurante.', 32, NULL, 1, 'ACTIVO'),
(11, 3, 'Anticuchos', 'Preparación de muestra para el prototipo del restaurante.', 20, NULL, 1, 'ACTIVO'),
(12, 4, 'Pollo a la brasa - medio', 'Preparación de muestra para el prototipo del restaurante.', 42, '/img/pollo-brasa.png', 1, 'ACTIVO'),
(13, 5, 'Caldo de gallina', 'Preparación de muestra para el prototipo del restaurante.', 18, NULL, 1, 'ACTIVO'),
(14, 5, 'Sopa de quinua', 'Preparación de muestra para el prototipo del restaurante.', 15, NULL, 1, 'ACTIVO'),
(15, 6, 'Ensalada fresca', 'Preparación de muestra para el prototipo del restaurante.', 12, NULL, 1, 'ACTIVO'),
(16, 6, 'Ensalada de palta', 'Preparación de muestra para el prototipo del restaurante.', 16, NULL, 1, 'ACTIVO'),
(17, 7, 'Limonada - jarra', 'Preparación de muestra para el prototipo del restaurante.', 15, NULL, 1, 'ACTIVO'),
(18, 7, 'Agua mineral', 'Preparación de muestra para el prototipo del restaurante.', 5, NULL, 1, 'ACTIVO'),
(19, 8, 'Arroz con leche', 'Preparación de muestra para el prototipo del restaurante.', 8, NULL, 1, 'ACTIVO'),
(20, 8, 'Helado artesanal', 'Preparación de muestra para el prototipo del restaurante.', 10, NULL, 0, 'ACTIVO');

INSERT INTO `pisos` (`id_piso`, `nombre`, `descripcion`, `estado`) VALUES
(1, 'Primer piso', 'Salón principal', 'ACTIVO'),
(2, 'Segundo piso', 'Salón familiar', 'ACTIVO'),
(3, 'Terraza', 'Zona al aire libre', 'ACTIVO');

INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES
(1, 1, 1, 4, 'OCUPADA'),
(2, 1, 2, 4, 'OCUPADA'),
(3, 1, 3, 4, 'RESERVADA'),
(4, 1, 4, 6, 'DISPONIBLE'),
(5, 2, 5, 4, 'OCUPADA'),
(6, 2, 6, 4, 'RESERVADA'),
(7, 2, 7, 4, 'OCUPADA'),
(8, 2, 8, 6, 'OCUPADA'),
(9, 3, 9, 4, 'DISPONIBLE'),
(10, 3, 10, 4, 'DISPONIBLE'),
(11, 3, 11, 4, 'RESERVADA'),
(12, 3, 12, 6, 'INACTIVA');

INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES
(1, 'LUNES', '00:00:00', '23:59:59', 1),
(2, 'MARTES', '00:00:00', '23:59:59', 1),
(3, 'MIERCOLES', '00:00:00', '23:59:59', 1),
(4, 'JUEVES', '00:00:00', '23:59:59', 1),
(5, 'VIERNES', '00:00:00', '23:59:59', 1),
(6, 'SABADO', '00:00:00', '23:59:59', 1),
(7, 'DOMINGO', '00:00:00', '23:59:59', 1);

INSERT INTO `configuracion_restaurante` (`id_configuracion`, `nombre`, `direccion`, `referencia`, `telefono`, `whatsapp`, `correo`, `numero_yape`, `titular_yape`, `numero_plin`, `titular_plin`, `banco_transferencia`, `titular_transferencia`, `numero_cuenta_transferencia`, `cci_transferencia`, `actualizado_en`) VALUES
(1, 'El Rinconcito Ayacuchano Campestre', 'Dirección de demostración - Ayacucho', 'Datos ficticios para capturas', '900000001', '900000001', 'demo@example.test', '900000001', 'Restaurante Demo', '900000002', 'Restaurante Demo', 'Banco de demostración', 'Restaurante Demo', '0000000000000', '00000000000000000000', @ahora);

INSERT INTO `direcciones` (`id_direccion`, `id_usuario`, `direccion`, `referencia`, `distrito`, `principal`) VALUES
(1, 4, 'Dirección de muestra 101', 'Referencia ficticia', 'Ayacucho', 1),
(2, 5, 'Dirección de muestra 202', 'Referencia ficticia', 'Ayacucho', 1);

INSERT INTO `eventos` (`id_evento`, `titulo`, `descripcion`, `fecha`, `hora_inicio`, `hora_fin`, `requiere_pago`, `precio_entrada`, `estado`) VALUES
(1, 'Almuerzo familiar', 'Evento de demostración', @hoy, @inicio, @fin, 0, 0, 'ACTIVO'),
(2, 'Tarde cultural', 'Evento de demostración con entrada', DATE_ADD(@hoy, INTERVAL 2 DAY), '12:00:00', '16:00:00', 1, 25, 'ACTIVO');

INSERT INTO `galeria` (`id_imagen`, `titulo`, `descripcion`, `ruta_imagen`, `fecha_publicacion`, `estado`) VALUES
(1, 'Pachamanca', 'Fotografía incluida en el proyecto', '/img/pachamanca.png', @ahora, 'ACTIVO'),
(2, 'Cuy chactado', 'Fotografía incluida en el proyecto', '/img/cuy-chactado.png', @ahora, 'ACTIVO'),
(3, 'Pollo a la brasa', 'Fotografía incluida en el proyecto', '/img/pollo-brasa.png', @ahora, 'ACTIVO');

INSERT INTO `reservas` (`id_reserva`, `codigo_reserva`, `id_cliente`, `id_piso_solicitado`, `id_mesa`, `fecha_reserva`, `hora_inicio`, `hora_fin`, `numero_personas`, `ocasion_especial`, `comentarios`, `requiere_pago`, `monto_reserva`, `estado`, `fecha_registro`) VALUES
(1, 'RES-DEMO-001', 5, 1, 3, @hoy, @inicio, @fin, 4, 'Almuerzo familiar', 'Datos ficticios para el prototipo', 0, 0, 'CONFIRMADA', @ahora),
(2, 'RES-DEMO-002', 6, 2, 6, @hoy, @inicio, @fin, 3, 'Reunión de amigos', 'Datos ficticios para el prototipo', 0, 0, 'CLIENTE_LLEGO', @ahora),
(3, 'RES-DEMO-003', 8, 2, 8, @hoy, @inicio, @fin, 4, 'Cumpleaños', 'Datos ficticios para el prototipo', 0, 0, 'EN_ATENCION', @ahora),
(4, 'RES-DEMO-004', 7, 3, 11, @hoy, @inicio, @fin, 4, 'Aniversario', 'Datos ficticios para el prototipo', 1, 30, 'CONFIRMADA', @ahora),
(5, 'RES-DEMO-005', 5, 3, 12, @ayer, '12:00:00', '14:00:00', 2, 'Reserva cancelada de ejemplo', 'Datos ficticios para el prototipo', 0, 0, 'CANCELADA', DATE_SUB(@ahora, INTERVAL 1 DAY)),
(6, 'RES-DEMO-006', 4, 3, 9, @ayer, '12:00:00', '14:00:00', 4, 'Atención finalizada', 'Datos ficticios para el prototipo', 0, 0, 'FINALIZADA', DATE_SUB(@ahora, INTERVAL 1 DAY));

INSERT INTO `pedidos` (`id_pedido`, `codigo_pedido`, `id_cliente`, `id_mesero`, `id_mesa`, `tipo_pedido`, `estado`, `subtotal`, `costo_delivery`, `total`, `observaciones`, `fecha_registro`, `fecha_actualizacion`) VALUES
(1, 'PED-DEMO-001', 4, 2, 1, 'PRESENCIAL', 'CONFIRMADO', 108, 0, 108, 'Una pachamanca sin ají. Bebidas al inicio.', DATE_SUB(@ahora, INTERVAL 30 MINUTE), @ahora),
(2, 'PED-DEMO-002', 5, 2, 2, 'PRESENCIAL', 'PENDIENTE_PAGO', 56, 0, 56, 'El cliente solicitó la cuenta.', DATE_SUB(@ahora, INTERVAL 30 MINUTE), @ahora),
(3, 'PED-DEMO-003', 6, 2, 5, 'PRESENCIAL', 'PENDIENTE_VALIDACION', 73, 0, 73, 'Pago enviado a Caja para validación.', DATE_SUB(@ahora, INTERVAL 30 MINUTE), @ahora),
(4, 'PED-DEMO-004', 4, 2, 9, 'PRESENCIAL', 'FINALIZADO', 112, 0, 112, 'Atención finalizada y cobrada.', DATE_SUB(@ahora, INTERVAL 1 DAY), DATE_SUB(@ahora, INTERVAL 1 DAY)),
(5, 'PED-DEMO-005', 5, 2, 10, 'PRESENCIAL', 'CANCELADO', 44, 0, 44, 'El cliente canceló antes de preparar.', DATE_SUB(@ahora, INTERVAL 1 DAY), DATE_SUB(@ahora, INTERVAL 1 DAY)),
(6, 'PED-DEMO-006', 8, 2, 8, 'PRESENCIAL', 'CONFIRMADO', 53, 0, 53, 'Reserva de cumpleaños: postre al final.', DATE_SUB(@ahora, INTERVAL 30 MINUTE), @ahora);

INSERT INTO `detalle_pedido` (`id_detalle`, `id_pedido`, `id_producto`, `cantidad`, `precio_unitario`, `subtotal`, `observaciones`) VALUES
(1, 1, 2, 2, 45, 90, 'Sin ají'),
(2, 1, 6, 3, 6, 18, NULL),
(3, 2, 5, 2, 22, 44, NULL),
(4, 2, 6, 2, 6, 12, NULL),
(5, 3, 3, 1, 55, 55, NULL),
(6, 3, 1, 1, 12, 12, NULL),
(7, 3, 6, 1, 6, 6, NULL),
(8, 4, 4, 2, 48, 96, NULL),
(9, 4, 7, 2, 8, 16, NULL),
(10, 5, 5, 2, 22, 44, NULL),
(11, 6, 2, 1, 45, 45, NULL),
(12, 6, 7, 1, 8, 8, NULL);

INSERT INTO `comandas` (`id_comanda`, `id_pedido`, `numero_comanda`, `fecha_emision`, `estado`, `observaciones`) VALUES
(1, 1, 'COM-DEMO-001', @ahora, 'EN_PREPARACION', 'Una pachamanca sin ají. Bebidas al inicio.'),
(2, 2, 'COM-DEMO-002', @ahora, 'ENTREGADA', 'El cliente solicitó la cuenta.'),
(3, 3, 'COM-DEMO-003', @ahora, 'ENTREGADA', 'Pago enviado a Caja para validación.'),
(4, 4, 'COM-DEMO-004', DATE_SUB(@ahora, INTERVAL 1 DAY), 'ENTREGADA', 'Atención finalizada y cobrada.'),
(5, 5, 'COM-DEMO-005', DATE_SUB(@ahora, INTERVAL 1 DAY), 'CANCELADA', 'El cliente canceló antes de preparar.'),
(6, 6, 'COM-DEMO-006', @ahora, 'PENDIENTE', 'Reserva de cumpleaños: postre al final.');

INSERT INTO `pagos` (`id_pago`, `id_pedido`, `id_reserva`, `metodo_pago`, `monto`, `numero_operacion`, `estado`, `id_usuario_validador`, `fecha_pago`, `fecha_validacion`) VALUES
(1, 3, NULL, 'YAPE', 73, 'DEMO-YAPE-001', 'PENDIENTE_VALIDACION', NULL, @ahora, NULL),
(2, 4, NULL, 'EFECTIVO', 112, NULL, 'APROBADO', 3, DATE_SUB(@ahora, INTERVAL 1 DAY), DATE_SUB(@ahora, INTERVAL 1 DAY)),
(3, NULL, 4, 'TRANSFERENCIA', 30, 'DEMO-TRANS-001', 'APROBADO', 3, @ahora, @ahora);

INSERT INTO `notas_venta` (`id_nota`, `id_pago`, `id_usuario_emisor`, `serie`, `numero`, `cliente_nombre`, `cliente_documento`, `total`, `fecha_emision`, `estado`) VALUES
(1, 2, 3, 'NV01', 1, 'Lucía Demo', '20000004', 112, DATE_SUB(@ahora, INTERVAL 1 DAY), 'EMITIDA'),
(2, 3, 3, 'NV01', 2, 'Andrés Demo', '20000007', 30, @ahora, 'EMITIDA');

COMMIT;

SELECT 'Productos' AS dato, COUNT(*) AS cantidad FROM productos
UNION ALL SELECT 'Mesas', COUNT(*) FROM mesas
UNION ALL SELECT 'Reservas', COUNT(*) FROM reservas
UNION ALL SELECT 'Pedidos', COUNT(*) FROM pedidos;
SELECT p.codigo_pedido, p.total, SUM(d.subtotal) AS suma_detalles
FROM pedidos p JOIN detalle_pedido d ON d.id_pedido=p.id_pedido GROUP BY p.id_pedido;

-- Configura en cada laptop un usuario MySQL con acceso a esta base.

-- Tablas incorporadas despues del esquema inicial.


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
