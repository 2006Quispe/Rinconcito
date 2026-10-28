# Rinconcito

Sistema web del restaurante con modulos de clientes, mozo, Caja y administracion.

- [Ejecutar en otra PC: base demo, cuentas y configuracion local](docs/EJECUTAR_EN_OTRA_PC.md).
- [Correo del negocio con Hotmail y Microsoft Graph](docs/CORREO_HOTMAIL.md).
- [Buzon simulado para probar sin enviar correos reales](docs/PRUEBA_CORREO.md).

Requiere JDK 21 o superior y MySQL 8. Arranque normal: `./mvnw spring-boot:run`
(o `.\mvnw.cmd spring-boot:run` en Windows). URL: http://localhost:8081.
El correo es opcional y esta deshabilitado por defecto; el resto del sistema funciona sin autorizarlo.
Las claves MySQL y los tokens se configuran en `.local/`, que no se sube a Git.
