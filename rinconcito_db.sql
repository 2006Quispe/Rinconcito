-- Exportacion compartible del proyecto Rinconcito Ayacuchano.
-- Contiene el esquema MySQL actual y datos ficticios para desarrollo universitario.
-- Se omitieron usuarios locales, direcciones, contacto/pago del restaurante y movimientos personales.
-- Importar una sola vez en MySQL Workbench en una instalacion limpia de MySQL.
-- Cuentas demo: admin@rinconcito.test, mesero@rinconcito.test, caja@rinconcito.test, cliente@rinconcito.test
-- Contrasena de prueba para esas cuentas: Prueba123! (solo desarrollo; cambiar antes de publicar).

-- MySQL dump 10.13  Distrib 26.10.0, for Win64 (x86_64)
--
-- Host: localhost    Database: rinconcito_db
-- ------------------------------------------------------
-- Server version	26.10.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `rinconcito_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `rinconcito_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `rinconcito_db`;

--
-- Table structure for table `categorias`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorias` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `uq_categorias_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_campanas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_campanas` (
  `id_campana` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `porcentaje_bonus` int NOT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime NOT NULL,
  `activa` tinyint(1) NOT NULL DEFAULT '1',
  `id_admin` int DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_campana`),
  KEY `idx_club_campanas_vigencia` (`activa`,`fecha_inicio`,`fecha_fin`),
  KEY `fk_club_campanas_admin` (`id_admin`),
  CONSTRAINT `fk_club_campanas_admin` FOREIGN KEY (`id_admin`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_club_campanas_fechas` CHECK ((`fecha_fin` >= `fecha_inicio`)),
  CONSTRAINT `chk_club_campanas_porcentaje` CHECK ((`porcentaje_bonus` between 1 and 500))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_canjes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_canjes` (
  `id_canje` bigint NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_recompensa` int NOT NULL,
  `id_pedido` int DEFAULT NULL,
  `codigo_cupon` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre_recompensa` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valor` int NOT NULL,
  `compra_minima` int NOT NULL DEFAULT '0',
  `puntos_usados` int NOT NULL,
  `condiciones` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoria` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DESCUENTOS',
  `id_usuario_validador` int DEFAULT NULL,
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DISPONIBLE',
  `fecha_canje` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `vence_en` datetime NOT NULL,
  `usado_en` datetime DEFAULT NULL,
  PRIMARY KEY (`id_canje`),
  UNIQUE KEY `uq_club_canjes_codigo` (`codigo_cupon`),
  UNIQUE KEY `uq_club_canjes_pedido` (`id_pedido`),
  KEY `idx_club_canjes_usuario_estado` (`id_usuario`,`estado`,`vence_en`),
  KEY `fk_club_canjes_recompensa` (`id_recompensa`),
  KEY `fk_club_canjes_validador` (`id_usuario_validador`),
  CONSTRAINT `fk_club_canjes_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  CONSTRAINT `fk_club_canjes_recompensa` FOREIGN KEY (`id_recompensa`) REFERENCES `club_recompensas` (`id_recompensa`),
  CONSTRAINT `fk_club_canjes_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_club_canjes_validador` FOREIGN KEY (`id_usuario_validador`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_club_canjes_puntos` CHECK ((`puntos_usados` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_configuracion`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_configuracion` (
  `clave` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valor` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `id_admin` int DEFAULT NULL,
  PRIMARY KEY (`clave`),
  KEY `fk_club_config_admin` (`id_admin`),
  CONSTRAINT `fk_club_config_admin` FOREIGN KEY (`id_admin`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_consumos_lotes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_consumos_lotes` (
  `id_canje` bigint NOT NULL,
  `id_lote` bigint NOT NULL,
  `puntos` int NOT NULL,
  PRIMARY KEY (`id_canje`,`id_lote`),
  KEY `fk_club_consumos_lote` (`id_lote`),
  CONSTRAINT `fk_club_consumos_canje` FOREIGN KEY (`id_canje`) REFERENCES `club_canjes` (`id_canje`),
  CONSTRAINT `fk_club_consumos_lote` FOREIGN KEY (`id_lote`) REFERENCES `club_lotes_puntos` (`id_lote`),
  CONSTRAINT `chk_club_consumos_puntos` CHECK ((`puntos` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_lotes_puntos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_lotes_puntos` (
  `id_lote` bigint NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_pago` int DEFAULT NULL,
  `codigo_origen` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `puntos_originales` int NOT NULL,
  `puntos_disponibles` int NOT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `vence_en` datetime DEFAULT NULL,
  PRIMARY KEY (`id_lote`),
  UNIQUE KEY `uq_club_lotes_codigo_origen` (`codigo_origen`),
  UNIQUE KEY `uq_club_lotes_pago` (`id_pago`),
  KEY `idx_club_lotes_vencimiento` (`id_usuario`,`vence_en`,`puntos_disponibles`),
  CONSTRAINT `fk_club_lotes_pago` FOREIGN KEY (`id_pago`) REFERENCES `pagos` (`id_pago`),
  CONSTRAINT `fk_club_lotes_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_club_lotes_disponibles` CHECK (((`puntos_disponibles` >= 0) and (`puntos_disponibles` <= `puntos_originales`))),
  CONSTRAINT `chk_club_lotes_originales` CHECK ((`puntos_originales` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_movimientos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_movimientos` (
  `id_movimiento` bigint NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_pago` int DEFAULT NULL,
  `id_canje` bigint DEFAULT NULL,
  `id_lote` bigint DEFAULT NULL,
  `id_admin` int DEFAULT NULL,
  `referencia_operacion` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vence_en` datetime DEFAULT NULL,
  `tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `puntos` int NOT NULL,
  `descripcion` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_movimiento`),
  UNIQUE KEY `uq_club_movimientos_pago` (`id_pago`),
  UNIQUE KEY `uq_club_movimientos_canje` (`id_canje`),
  UNIQUE KEY `uq_club_movimientos_referencia` (`referencia_operacion`),
  KEY `idx_club_movimientos_usuario_fecha` (`id_usuario`,`creado_en`),
  KEY `fk_club_movimientos_lote` (`id_lote`),
  KEY `fk_club_movimientos_admin` (`id_admin`),
  CONSTRAINT `fk_club_movimientos_admin` FOREIGN KEY (`id_admin`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_club_movimientos_canje` FOREIGN KEY (`id_canje`) REFERENCES `club_canjes` (`id_canje`),
  CONSTRAINT `fk_club_movimientos_lote` FOREIGN KEY (`id_lote`) REFERENCES `club_lotes_puntos` (`id_lote`),
  CONSTRAINT `fk_club_movimientos_pago` FOREIGN KEY (`id_pago`) REFERENCES `pagos` (`id_pago`),
  CONSTRAINT `fk_club_movimientos_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_recompensas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_recompensas` (
  `id_recompensa` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `costo_puntos` int NOT NULL,
  `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valor` int NOT NULL,
  `compra_minima` int NOT NULL DEFAULT '0',
  `imagen` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categoria` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DESCUENTOS',
  `condiciones` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vigencia_dias` int NOT NULL DEFAULT '90',
  `id_producto` int DEFAULT NULL,
  `precio_referencia` int DEFAULT NULL,
  `disponible_desde` datetime DEFAULT NULL,
  `disponible_hasta` datetime DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `stock` int DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_recompensa`),
  UNIQUE KEY `uq_club_recompensas_codigo` (`codigo`),
  KEY `fk_club_recompensas_producto` (`id_producto`),
  CONSTRAINT `fk_club_recompensas_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE SET NULL,
  CONSTRAINT `chk_club_recompensas_costo` CHECK ((`costo_puntos` > 0)),
  CONSTRAINT `chk_club_recompensas_valor` CHECK ((`valor` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `club_saldos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_saldos` (
  `id_usuario` int NOT NULL,
  `puntos` int NOT NULL DEFAULT '0',
  `puntos_deuda` int NOT NULL DEFAULT '0',
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  CONSTRAINT `fk_club_saldos_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `chk_club_saldos_puntos` CHECK ((`puntos` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comandas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comandas` (
  `id_comanda` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `numero_comanda` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('PENDIENTE','EN_PREPARACION','LISTA','ENTREGADA','CANCELADA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE',
  `observaciones` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_comanda`),
  UNIQUE KEY `uq_comandas_numero` (`numero_comanda`),
  KEY `fk_comandas_pedido` (`id_pedido`),
  CONSTRAINT `fk_comandas_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `configuracion_restaurante`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `configuracion_restaurante` (
  `id_configuracion` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `direccion` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `referencia` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `whatsapp` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_google_maps` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_facebook` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url_instagram` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_yape` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_yape` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qr_yape` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_plin` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_plin` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qr_plin` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `banco_transferencia` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `titular_transferencia` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_cuenta_transferencia` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cci_transferencia` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_configuracion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `correo_salida`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `correo_salida` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `clave` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_referencia` int NOT NULL,
  `destinatario` varchar(254) COLLATE utf8mb4_unicode_ci NOT NULL,
  `asunto` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cuerpo` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE',
  `intentos` int NOT NULL DEFAULT '0',
  `error` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `enviado` datetime DEFAULT NULL,
  `proximo_intento` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `clave` (`clave`),
  KEY `idx_correo_pendiente` (`estado`,`proximo_intento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `detalle_pedido`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detalle_pedido` (
  `id_detalle` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `id_producto` int DEFAULT NULL,
  `cantidad` int NOT NULL,
  `precio_unitario` int NOT NULL,
  `subtotal` int NOT NULL,
  `nombre_producto` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `es_recompensa_club` tinyint(1) NOT NULL DEFAULT '0',
  `descuento_club` int NOT NULL DEFAULT '0',
  `observaciones` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_detalle`),
  KEY `fk_detalle_pedido` (`id_pedido`),
  KEY `fk_detalle_producto` (`id_producto`),
  CONSTRAINT `fk_detalle_pedido` FOREIGN KEY (`id_pedido`) REFERENCES `pedidos` (`id_pedido`),
  CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  CONSTRAINT `chk_detalle_cantidad` CHECK ((`cantidad` > 0)),
  CONSTRAINT `chk_detalle_precio` CHECK (((`precio_unitario` >= 0) and (`subtotal` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `direcciones`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `direcciones` (
  `id_direccion` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `direccion` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `referencia` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distrito` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitud` decimal(10,7) DEFAULT NULL,
  `longitud` decimal(10,7) DEFAULT NULL,
  `principal` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_direccion`),
  KEY `fk_direcciones_usuario` (`id_usuario`),
  CONSTRAINT `fk_direcciones_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventos` (
  `id_evento` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha` date NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time DEFAULT NULL,
  `imagen` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pago` tinyint(1) NOT NULL DEFAULT '0',
  `precio_entrada` int NOT NULL DEFAULT '0',
  `estado` enum('ACTIVO','INACTIVO','CANCELADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_evento`),
  KEY `idx_eventos_fecha_estado` (`fecha`,`estado`),
  CONSTRAINT `chk_eventos_precio` CHECK ((`precio_entrada` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `galeria`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `galeria` (
  `id_imagen` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ruta_imagen` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_publicacion` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_imagen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `historial_disponibilidad_producto`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_disponibilidad_producto` (
  `id_historial` bigint NOT NULL AUTO_INCREMENT,
  `id_producto` int NOT NULL,
  `id_usuario` int NOT NULL,
  `disponible_anterior` tinyint(1) NOT NULL,
  `disponible_nuevo` tinyint(1) NOT NULL,
  `fecha_cambio` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id_historial`),
  KEY `idx_historial_disponibilidad_fecha` (`fecha_cambio`),
  KEY `idx_historial_disponibilidad_producto` (`id_producto`),
  KEY `idx_historial_disponibilidad_usuario` (`id_usuario`),
  CONSTRAINT `fk_historial_disponibilidad_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_historial_disponibilidad_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `horarios_atencion`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `horarios_atencion` (
  `id_horario` int NOT NULL AUTO_INCREMENT,
  `dia_semana` enum('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hora_apertura` time DEFAULT NULL,
  `hora_cierre` time DEFAULT NULL,
  `abierto` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_horario`),
  UNIQUE KEY `uq_horarios_dia` (`dia_semana`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mesas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mesas` (
  `id_mesa` int NOT NULL AUTO_INCREMENT,
  `id_piso` int NOT NULL,
  `numero` int NOT NULL,
  `capacidad` int NOT NULL,
  `estado` enum('DISPONIBLE','OCUPADA','RESERVADA','INACTIVA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DISPONIBLE',
  PRIMARY KEY (`id_mesa`),
  UNIQUE KEY `uq_mesas_piso_numero` (`id_piso`,`numero`),
  KEY `idx_mesas_piso_estado` (`id_piso`,`estado`),
  CONSTRAINT `fk_mesas_piso` FOREIGN KEY (`id_piso`) REFERENCES `pisos` (`id_piso`),
  CONSTRAINT `chk_mesas_capacidad` CHECK ((`capacidad` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notas_venta`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notas_venta` (
  `id_nota` int NOT NULL AUTO_INCREMENT,
  `id_pago` int NOT NULL,
  `id_usuario_emisor` int NOT NULL,
  `serie` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NV01',
  `numero` int NOT NULL,
  `cliente_nombre` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cliente_documento` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total` int NOT NULL,
  `fecha_emision` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` enum('EMITIDA','ANULADA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMITIDA',
  `motivo_anulacion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_anulacion` datetime DEFAULT NULL,
  PRIMARY KEY (`id_nota`),
  UNIQUE KEY `uq_notas_venta_pago` (`id_pago`),
  UNIQUE KEY `uq_notas_venta_serie_numero` (`serie`,`numero`),
  KEY `fk_notas_venta_emisor` (`id_usuario_emisor`),
  CONSTRAINT `fk_notas_venta_emisor` FOREIGN KEY (`id_usuario_emisor`) REFERENCES `usuarios` (`id_usuario`),
  CONSTRAINT `fk_notas_venta_pago` FOREIGN KEY (`id_pago`) REFERENCES `pagos` (`id_pago`),
  CONSTRAINT `chk_notas_venta_total` CHECK ((`total` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pagos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pagos` (
  `id_pago` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int DEFAULT NULL,
  `id_reserva` int DEFAULT NULL,
  `metodo_pago` enum('YAPE','PLIN','EFECTIVO','TRANSFERENCIA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `monto` int NOT NULL,
  `numero_operacion` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evidencia_pago` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('PENDIENTE_VALIDACION','APROBADO','RECHAZADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  `id_usuario_validador` int DEFAULT NULL,
  `fecha_pago` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_validacion` datetime DEFAULT NULL,
  `observacion_validacion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pedidos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedidos` (
  `id_pedido` int NOT NULL AUTO_INCREMENT,
  `codigo_pedido` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_cliente` int NOT NULL,
  `id_mesero` int DEFAULT NULL,
  `id_mesa` int DEFAULT NULL,
  `id_direccion` int DEFAULT NULL,
  `tipo_pedido` enum('DELIVERY','PRESENCIAL','RECOJO_LOCAL') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado` enum('PENDIENTE_PAGO','PENDIENTE_VALIDACION','CONFIRMADO','CANCELADO','FINALIZADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE_PAGO',
  `subtotal` int NOT NULL DEFAULT '0',
  `costo_delivery` int NOT NULL DEFAULT '0',
  `descuento_club` int NOT NULL DEFAULT '0',
  `total` int NOT NULL DEFAULT '0',
  `observaciones` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pisos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pisos` (
  `id_piso` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_piso`),
  UNIQUE KEY `uq_pisos_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `productos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `productos` (
  `id_producto` int NOT NULL AUTO_INCREMENT,
  `id_categoria` int NOT NULL,
  `nombre` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ingredientes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `precio` int NOT NULL,
  `imagen` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `disponible` tinyint(1) NOT NULL DEFAULT '1',
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_producto`),
  KEY `idx_productos_categoria` (`id_categoria`),
  CONSTRAINT `fk_productos_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`),
  CONSTRAINT `chk_productos_precio` CHECK ((`precio` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reservas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservas` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `codigo_reserva` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_cliente` int NOT NULL,
  `id_piso_solicitado` int NOT NULL,
  `id_mesa` int DEFAULT NULL,
  `id_evento` int DEFAULT NULL,
  `fecha_reserva` date NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  `numero_personas` int NOT NULL,
  `ocasion_especial` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comentarios` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requiere_pago` tinyint(1) NOT NULL DEFAULT '0',
  `monto_reserva` int NOT NULL DEFAULT '0',
  `estado` enum('SOLICITADA','PENDIENTE_PAGO','PENDIENTE_VALIDACION','CONFIRMADA','CLIENTE_LLEGO','EN_ATENCION','FINALIZADA','CANCELADA','RECHAZADA') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SOLICITADA',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id_rol` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `uq_roles_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuarios`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `id_rol` int NOT NULL,
  `nombres` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `apellidos` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `dni` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(9) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contrasena` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_registro` enum('CUENTA','INVITADO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUENTA',
  `estado` enum('ACTIVO','INACTIVO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVO',
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuarios_dni` (`dni`),
  UNIQUE KEY `uq_usuarios_correo` (`correo`),
  KEY `idx_usuarios_rol` (`id_rol`),
  CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'rinconcito_db'
--

-- Table structure for table `club_pago_resumen`
--
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `club_pago_resumen` (
  `id_pago` int NOT NULL,
  `id_usuario` int NOT NULL,
  `nivel_codigo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nivel_nombre` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `puntos_anuales_antes` int NOT NULL DEFAULT '0',
  `saldo_anterior` int NOT NULL DEFAULT '0',
  `deuda_anterior` int NOT NULL DEFAULT '0',
  `puntos_base` int NOT NULL DEFAULT '0',
  `puntos_bonus_nivel` int NOT NULL DEFAULT '0',
  `puntos_bonus_campana` int NOT NULL DEFAULT '0',
  `campana_nombre` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `puntos_totales` int NOT NULL DEFAULT '0',
  `puntos_aplicados_deuda` int NOT NULL DEFAULT '0',
  `puntos_acreditados` int NOT NULL DEFAULT '0',
  `saldo_nuevo` int NOT NULL DEFAULT '0',
  `deuda_nueva` int NOT NULL DEFAULT '0',
  `nuevo_nivel_codigo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nuevo_nivel_nombre` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pago`),
  KEY `idx_club_pago_resumen_usuario` (`id_usuario`,`creado_en`),
  CONSTRAINT `fk_club_pago_resumen_pago` FOREIGN KEY (`id_pago`) REFERENCES `pagos` (`id_pago`),
  CONSTRAINT `fk_club_pago_resumen_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'rinconcito_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-26 12:30:27

SET FOREIGN_KEY_CHECKS=0;
INSERT INTO `roles` (`id_rol`, `nombre`) VALUES (1,'ADMINISTRADOR');
INSERT INTO `roles` (`id_rol`, `nombre`) VALUES (3,'CAJA');
INSERT INTO `roles` (`id_rol`, `nombre`) VALUES (4,'CLIENTE');
INSERT INTO `roles` (`id_rol`, `nombre`) VALUES (2,'MESERO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (1,'Entradas','Opciones de entradas','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (2,'Platos tÃ­picos','Opciones de platos tÃ­picos','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (3,'Parrillas','Opciones de parrillas','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (4,'Pollo a la brasa','Opciones de pollo a la brasa','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (5,'Sopas','Opciones de sopas','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (6,'Ensaladas','Opciones de ensaladas','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (7,'Bebidas','Opciones de bebidas','ACTIVO');
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`, `estado`) VALUES (8,'Postres','Opciones de postres','ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (1,1,'Papa a la huancaÃ­na','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,12,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (2,2,'Pachamanca especial','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,45,'/img/pachamanca.png',1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (3,2,'Cuy chactado','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,55,'/img/cuy-chactado.png',1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (4,3,'Parrilla mixta','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,48,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (5,4,'Pollo a la brasa - cuarto','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,22,'/img/pollo-brasa.png',1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (6,7,'Chicha morada','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,6,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (7,8,'Mazamorra morada','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,8,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (8,1,'Ocopa ayacuchana','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,12,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (9,2,'Puca picante con chicharrÃ³n','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,30,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (10,2,'Lomo saltado','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,32,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (11,3,'Anticuchos','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,20,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (12,4,'Pollo a la brasa - medio','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,42,'/img/pollo-brasa.png',1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (13,5,'Caldo de gallina','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,18,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (14,5,'Sopa de quinua','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,15,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (15,6,'Ensalada fresca','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,12,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (16,6,'Ensalada de palta','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,16,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (17,7,'Limonada - jarra','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,15,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (18,7,'Agua mineral','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,5,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (19,8,'Arroz con leche','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,8,NULL,1,'ACTIVO');
INSERT INTO `productos` (`id_producto`, `id_categoria`, `nombre`, `descripcion`, `ingredientes`, `precio`, `imagen`, `disponible`, `estado`) VALUES (20,8,'Helado artesanal','PreparaciÃ³n de muestra para el prototipo del restaurante.',NULL,10,NULL,0,'ACTIVO');
INSERT INTO `pisos` (`id_piso`, `nombre`, `descripcion`, `estado`) VALUES (1,'Primer piso','SalÃ³n principal','ACTIVO');
INSERT INTO `pisos` (`id_piso`, `nombre`, `descripcion`, `estado`) VALUES (2,'Segundo piso','SalÃ³n familiar','ACTIVO');
INSERT INTO `pisos` (`id_piso`, `nombre`, `descripcion`, `estado`) VALUES (3,'Terraza','Zona al aire libre','ACTIVO');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (1,1,1,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (2,1,2,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (3,1,3,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (4,1,4,6,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (5,2,5,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (6,2,6,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (7,2,7,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (8,2,8,6,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (9,3,9,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (10,3,10,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (11,3,11,4,'DISPONIBLE');
INSERT INTO `mesas` (`id_mesa`, `id_piso`, `numero`, `capacidad`, `estado`) VALUES (12,3,12,6,'INACTIVA');
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (1,'LUNES','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (2,'MARTES','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (3,'MIERCOLES','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (4,'JUEVES','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (5,'VIERNES','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (6,'SABADO','00:00:00','23:59:59',1);
INSERT INTO `horarios_atencion` (`id_horario`, `dia_semana`, `hora_apertura`, `hora_cierre`, `abierto`) VALUES (7,'DOMINGO','00:00:00','23:59:59',1);
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (1,'DESCUENTO-5','S/ 5 de descuento','Descuenta S/ 5 en un pedido online desde S/ 30.',100,'DESCUENTO_FIJO',5,30,'/img/club/descuento-5.webp','DESCUENTOS',NULL,90,NULL,NULL,NULL,1,NULL,'2026-11-25 16:16:30');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (2,'DESCUENTO-10','10% de descuento','Ahorra 10% en un pedido online desde S/ 50.',180,'DESCUENTO_PORCENTAJE',10,50,'/img/club/descuento-10.webp','DESCUENTOS',NULL,90,NULL,NULL,NULL,1,NULL,'2026-11-25 16:16:30');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (3,'DESCUENTO-20','S/ 20 de descuento','Descuenta S/ 20 en un pedido online desde S/ 100.',350,'DESCUENTO_FIJO',20,100,'/img/club/descuento-20.webp','DESCUENTOS',NULL,90,NULL,NULL,NULL,1,NULL,'2026-11-25 16:16:30');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (4,'DELIVERY-GRATIS','Delivery gratis','Canjea este cupÃ³n para cubrir la tarifa de delivery de un pedido online.',100,'DELIVERY_GRATIS',1,0,'/img/club/delivery-gratis.webp','DESCUENTOS',NULL,90,NULL,NULL,NULL,1,NULL,'2026-11-25 16:49:34');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `precio_referencia`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (5,'TAZA-LOGO','Taza con logo Rinconcito','Recompensa fÃ­sica para recoger en caja. Presenta tu cÃ³digo de canje.',500,'PRODUCTO_MERCH',1,0,'/img/club/taza-logo.webp','MERCHANDISING','VÃ¡lido para recojo en el restaurante. Presenta tu cÃ³digo en caja.',90,NULL,25,NULL,NULL,1,10,'2026-11-25 16:49:34');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `precio_referencia`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (6,'GORRA-LOGO','Gorra con logo Rinconcito','Recompensa fÃ­sica para recoger en caja. Presenta tu cÃ³digo de canje.',700,'PRODUCTO_MERCH',1,0,'/img/club/gorra-logo.webp','MERCHANDISING','VÃ¡lido para recojo en el restaurante. Presenta tu cÃ³digo en caja.',90,NULL,35,NULL,NULL,1,6,'2026-11-25 16:49:34');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `precio_referencia`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (7,'POLO-LOGO','Polo con logo Rinconcito','Recompensa fÃ­sica para recoger en caja. Presenta tu cÃ³digo de canje.',900,'PRODUCTO_MERCH',1,0,'/img/club/polo-logo.webp','MERCHANDISING','VÃ¡lido para recojo en el restaurante. Presenta tu cÃ³digo en caja.',90,NULL,45,NULL,NULL,1,5,'2026-11-25 16:49:34');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (26,'CHICHA-MORADA','Chicha morada','Recompensa de la carta: Chicha morada.',120,'PRODUCTO_CARTA',1,0,'/img/club/chicha-morada.webp','BEBIDAS_POSTRES','Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',90,6,NULL,NULL,1,NULL,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (27,'MAZAMORRA-MORADA','Mazamorra morada','Recompensa de la carta: Mazamorra morada.',150,'PRODUCTO_CARTA',1,0,'/img/club/mazamorra-morada.webp','BEBIDAS_POSTRES','Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',90,7,NULL,NULL,1,NULL,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (28,'OCOPA-AYACUCHANA','Ocopa ayacuchana','Recompensa de la carta: Ocopa ayacuchana.',200,'PRODUCTO_CARTA',1,0,'/img/club/ocopa-ayacuchana.webp','PLATOS_TIPICOS','Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta.',90,8,NULL,NULL,1,NULL,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (29,'PACHAMANCA-ESPECIAL','Pachamanca especial','Recompensa de la carta: Pachamanca especial.',450,'PRODUCTO_CARTA',1,0,'/img/pachamanca.png','PLATOS_TIPICOS','Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta. Canje disponible únicamente de lunes a viernes.',90,2,NULL,NULL,1,NULL,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (30,'CUY-CHACTADO','Cuy chactado','Recompensa de la carta: Cuy chactado.',550,'PRODUCTO_CARTA',1,0,'/img/cuy-chactado.png','PLATOS_TIPICOS','Canje por una unidad del producto indicado. Sujeto a disponibilidad de la carta. Canje disponible únicamente de lunes a viernes.',90,3,NULL,NULL,1,NULL,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (31,'FUENTE-COMPARTIR','Fuente para compartir','Recompensa pendiente de configuraciÃ³n de producto y disponibilidad.',900,'EXPERIENCIA',1,0,NULL,'EXPERIENCIAS_FAMILIARES','Debe configurarse y activarse desde AdministraciÃ³n.',90,NULL,NULL,NULL,0,0,'2026-11-25 23:01:08');
INSERT INTO `club_recompensas` (`id_recompensa`, `codigo`, `nombre`, `descripcion`, `costo_puntos`, `tipo`, `valor`, `compra_minima`, `imagen`, `categoria`, `condiciones`, `vigencia_dias`, `id_producto`, `disponible_desde`, `disponible_hasta`, `activo`, `stock`, `creado_en`) VALUES (32,'ALMUERZO-FAMILIAR','Almuerzo familiar','Recompensa pendiente de configuraciÃ³n de experiencia y disponibilidad.',1800,'EXPERIENCIA',1,0,NULL,'EXPERIENCIAS_FAMILIARES','Debe configurarse y activarse desde AdministraciÃ³n.',90,NULL,NULL,NULL,0,0,'2026-11-25 23:01:08');
INSERT INTO `club_configuracion` (`clave`, `valor`, `actualizado_en`, `id_admin`) VALUES ('MIGRACION_TAZA_500_APLICADA','1','2026-11-25 23:01:08',NULL);
INSERT INTO `club_configuracion` (`clave`, `valor`, `actualizado_en`, `id_admin`) VALUES ('PUNTOS_POR_SOL','1','2026-11-25 22:58:45',NULL);
INSERT INTO `club_configuracion` (`clave`, `valor`, `actualizado_en`, `id_admin`) VALUES ('VIGENCIA_PUNTOS_DIAS','365','2026-11-25 22:58:45',NULL);
INSERT INTO `club_configuracion` (`clave`, `valor`, `actualizado_en`, `id_admin`) VALUES ('VIGENCIA_RECOMPENSAS_DIAS','90','2026-11-25 22:58:45',NULL);
INSERT INTO configuracion_restaurante (id_configuracion, nombre, direccion, referencia, telefono, whatsapp, correo, url_google_maps, url_facebook, url_instagram, numero_yape, titular_yape, qr_yape, numero_plin, titular_plin, qr_plin, banco_transferencia, titular_transferencia, numero_cuenta_transferencia, cci_transferencia)
VALUES (1, 'Rinconcito Ayacuchano', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO usuarios (id_usuario, id_rol, nombres, apellidos, dni, telefono, correo, contrasena, tipo_registro, estado, fecha_registro) VALUES
(1, 1, 'Admin', 'Demo', '90000001', '900000001', 'admin@rinconcito.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', '2026-01-01 12:00:00'),
(2, 2, 'Mesero', 'Demo', '90000002', '900000002', 'mesero@rinconcito.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', '2026-01-01 12:00:00'),
(3, 3, 'Caja', 'Demo', '90000003', '900000003', 'caja@rinconcito.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', '2026-01-01 12:00:00'),
(4, 4, 'Cliente', 'Demo', '90000004', '900000004', 'cliente@rinconcito.test', '$2a$10$.4f.0mr/.cUVJ5GflLYKfeRhOd4xIQBbHLfnbzMwxKlmlygKrd4rm', 'CUENTA', 'ACTIVO', '2026-01-01 12:00:00');

INSERT INTO club_saldos (id_usuario, puntos, puntos_deuda) VALUES (4, 0, 0);


-- Eventos ficticios/demostrativos para mostrar la cartelera actual y próximos afiches.
SET NAMES utf8mb4;

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Año 2026', 'Celebra el Año Nuevo con Jade Perú, Alan Vásquez, Madeleine Quinto, Diamante Andino y Mario Gómez. Animación: Wallpa Waccay. Entrada libre. Comidas típicas, pollos a la brasa, juegos inflables y estacionamiento.', '2026-01-01', '11:00:00', NULL, '/img/eventos/evento-2026-01-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Año 2026' AND e.fecha = '2026-01-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2026', 'Almuerzo show con Diosdado Gaitán Castro, Los Runas y más artistas en el local más carnavalero. Animación: Wallpa Waccay.', '2026-02-01', '11:00:00', NULL, '/img/eventos/evento-2026-02-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Carnaval Ayacuchano: El Gigante Chuta Chutay 2026' AND e.fecha = '2026-02-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT '33 Aniversario de Canto Andino', 'Gran celebración con Surandino, Max Castro, Porfirio Ayvar y muchos artistas más. Venta de entradas por VAOPE.', '2026-04-19', '11:00:00', NULL, '/img/eventos/evento-2026-04-19.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = '33 Aniversario de Canto Andino' AND e.fecha = '2026-04-19');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Día del Trabajo', 'Show en vivo con Aycampito, Madeleine Quinto, Pío Quispe y Mario Gómez. Animación: Wallpa Waccay. Entrada libre.', '2026-05-01', '11:00:00', NULL, '/img/eventos/evento-2026-05-01.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Día del Trabajo' AND e.fecha = '2026-05-01');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Homenaje a Mamá', 'Celebra el Día de la Madre con Max Castro, Surandino, Diamante Andino, Mario Gómez y Alan Vásquez. Animación: Wallpa Waccay. Entrada libre.', '2026-05-10', '11:00:00', NULL, '/img/eventos/evento-2026-05-10.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Homenaje a Mamá' AND e.fecha = '2026-05-10');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Alan Vásquez y Chinita Oshiro', 'Domingo de música con Alan Vásquez, Chinita Oshiro y Madeleine Quinto. Animación: Wallpa Waccay. Entrada libre.', '2026-05-31', '11:00:00', NULL, '/img/eventos/evento-2026-05-31.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Alan Vásquez y Chinita Oshiro' AND e.fecha = '2026-05-31');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Feliz Día del Padre', 'Festejamos a papá con un almuerzo show lleno de música y sabor ayacuchano. Animación: Wallpa Waccay.', '2026-06-21', '11:00:00', NULL, '/img/eventos/evento-2026-06-21.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Feliz Día del Padre' AND e.fecha = '2026-06-21');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show en vivo con Kenny Hidalgo', 'El fenomenal Kenny Hidalgo, Liz Campos, Madeleine Quinto, Mario Gómez y Diamante Andino. Animación: Wallpa Waccay. Entrada libre.', '2026-08-02', '11:00:00', NULL, '/img/eventos/evento-2026-08-02.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show en vivo con Kenny Hidalgo' AND e.fecha = '2026-08-02');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show en vivo con Alan Vásquez', 'Alan Vásquez, Angélica María y Liz Campos en un domingo de música en vivo. Animación: Wallpa Waccay. Entrada libre.', '2026-10-18', '11:00:00', NULL, '/img/eventos/evento-2026-10-18.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show en vivo con Alan Vásquez' AND e.fecha = '2026-10-18');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT '38 Aniversario de El Rinconcito Ayacuchano', 'Celebramos 38 años con Surandino, Dúo Ayacucho, Los Runas, Marifor Gómez, Super Brillantes y Huacchas. Animación: Wallpa Waccay. Venta de entradas por VAOPE.', '2026-10-25', '13:00:00', NULL, '/img/eventos/evento-2026-10-25.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = '38 Aniversario de El Rinconcito Ayacuchano' AND e.fecha = '2026-10-25');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Dina Paucar', 'Dina Paucar, Angélica María y Chinita Oshiro. Animación: Wallpa Waccay. Entrada libre.', '2026-11-08', '11:00:00', NULL, '/img/eventos/evento-2026-11-08.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Dina Paucar' AND e.fecha = '2026-11-08');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Jade Perú y Hnos. Ayvar', 'Jade Perú, Hnos. Ayvar y Dianita del Perú. Animación: Wallpa Waccay. Entrada libre.', '2026-11-15', '11:00:00', NULL, '/img/eventos/evento-2026-11-15.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Jade Perú y Hnos. Ayvar' AND e.fecha = '2026-11-15');

INSERT INTO eventos (titulo, descripcion, fecha, hora_inicio, hora_fin, imagen, requiere_pago, precio_entrada, estado)
SELECT 'Show con Ayrampito, el Rey de los Carnavales', 'Ayrampito y María López en un gran show de domingo. Animación: Wallpa Waccay. Entrada libre.', '2026-11-22', '11:00:00', NULL, '/img/eventos/evento-2026-11-22.jpg', 0, 0, 'ACTIVO'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM eventos e WHERE e.titulo = 'Show con Ayrampito, el Rey de los Carnavales' AND e.fecha = '2026-11-22');

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

SET FOREIGN_KEY_CHECKS=1;

-- Modalidades de merchandising para la instalaci?n de demostraci?n.
UPDATE club_recompensas
SET descripcion = 'Recompensa exclusiva del Club: presencial, delivery o recojo en local.',
    condiciones = 'Disponible presencialmente, en delivery o recojo en local.'
WHERE codigo IN ('TAZA-LOGO', 'GORRA-LOGO', 'POLO-LOGO');
