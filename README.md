# Rinconcito

Sistema web del restaurante con modulos de clientes, mozo, Caja y administracion.

- [Ejecutar en otra PC: base demo, cuentas y configuracion local](docs/EJECUTAR_EN_OTRA_PC.md).
- [Correo del negocio con Hotmail y Microsoft Graph](docs/CORREO_HOTMAIL.md).
- [Buzon simulado para probar sin enviar correos reales](docs/PRUEBA_CORREO.md).
- [Actualizar y usar Club Rinconcito](docs/CLUB_RINCONCITO.md).
- [Verificación de consolidación y limitaciones](docs/VERIFICACION_CONSOLIDACION.md).

Requiere JDK 21 o superior y MySQL 8. Arranque normal: `./mvnw spring-boot:run`
(o `.\mvnw.cmd spring-boot:run` en Windows). URL: http://localhost:8081.
El correo es opcional y esta deshabilitado por defecto; el resto del sistema funciona sin autorizarlo.
Las claves MySQL y los tokens se configuran en `.local/`, que no se sube a Git.

## Entorno Java y Maven

El proyecto declara Java 21 en `pom.xml`; se recomienda usar JDK 21 para compilar y ejecutar. En Windows, configura `JAVA_HOME` al JDK instalado y antepón `%JAVA_HOME%\bin` a `PATH`. Comprueba `java -version` y `mvn -version` para confirmar que ambos usan el mismo JDK.

Usa una única caché Maven en `%USERPROFILE%\.m2\repository`. No crees cachés bajo `target/` ni dentro del repositorio. Si Java informa `user.home` como `C:\`, ejecuta Maven con `-Duser.home=%USERPROFILE% -Dmaven.repo.local=%USERPROFILE%\.m2\repository` para que lea y descargue dependencias en la caché estándar del usuario. La cuenta de Windows necesita permisos de lectura y escritura en esa carpeta.

El wrapper `mvnw.cmd` configura `user.home` y `maven.repo.local` desde `%USERPROFILE%` para usar una sola caché en Windows. Si ejecutas una instalación externa de `mvn` donde `user.home` apunta a `C:\`, agrega `-Duser.home=%USERPROFILE% -Dmaven.repo.local=%USERPROFILE%\.m2\repository`.
