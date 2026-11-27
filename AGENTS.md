# Instrucciones del proyecto Rinconcito Ayacuchano

## Contexto y autorización

Este es un proyecto universitario grupal en desarrollo, no un sistema en producción. El usuario autoriza trabajar directamente sobre el proyecto local y la base MySQL de desarrollo `rinconcito_db`, incluidas modificaciones necesarias al código y a los datos/esquema. No se requiere volver a pedir autorización para operaciones ordinarias dentro del proyecto y esa base.

Utiliza la conexión existente en `.local/application.properties`. No copies credenciales a archivos compartidos, logs, capturas o SQL exportable. No sustituyas MySQL por otro motor para el uso normal del aplicativo.

## Cambios en MySQL y sincronización

- Antes de operaciones destructivas o cambios estructurales importantes, crea y comprueba un respaldo local completo de `rinconcito_db`. Mantén los respaldos fuera del archivo SQL compartible y fuera de Git.
- Antes de implementar, inspecciona el código y el esquema MySQL actual. Mantén entidades, consultas, servicios y esquema sincronizados.
- Aplica directamente a MySQL las modificaciones necesarias y conserva en el proyecto los scripts SQL útiles para documentar o actualizar una instalación existente.
- Evita crear un archivo de migración por cada ajuste pequeño. Agrupa cambios relacionados en un script razonable y documenta cuáles scripts son históricos, opcionales o necesarios.
- Revisa referencias en el código y la documentación antes de borrar scripts. No elimines scripts de los que dependan el aplicativo, el flujo de actualización o la demostración del grupo.
- Después de una implementación, comprueba su funcionamiento con el MySQL conectado. Prefiere verificaciones puntuales del flujo afectado; no ejecutes la suite completa salvo que el usuario lo solicite o sea necesaria para resolver un problema.

## Archivo SQL compartible

Mantén en la raíz del proyecto un único archivo canónico llamado `rinconcito_db.sql` para que los compañeros puedan importar la base completa en MySQL Workbench. Debe incluir la estructura actual y datos de demostración suficientes para usar el aplicativo.

Al actualizarlo:

- Exporta solamente datos ficticios o demostrativos. Excluye cuentas reales, datos personales, direcciones particulares, pagos, números de cuenta, CCI, teléfonos privados, contraseñas privadas y cualquier otra información sensible.
- Si hacen falta cuentas de demostración, utiliza únicamente identidades y credenciales ficticias de prueba, claramente identificables como tales; nunca exportes cuentas locales reales ni hashes de sus contraseñas.
- No incluyas contraseñas de MySQL, usuarios/grants del servidor, rutas locales, archivos `.local` ni secretos de correo o servicios externos.
- Comprueba que el SQL contiene la estructura y las semillas necesarias y que puede importarse en una base limpia compatible con MySQL.
- Conserva los scripts SQL históricos u opcionales que el aplicativo, la documentación o la instalación alternativa todavía utilicen; `rinconcito_db.sql` debe permitir una instalación nueva sin ejecutar múltiples migraciones.

## Ejecución y entrega

Compila, inicia o reinicia el aplicativo cuando corresponda y verifica el comportamiento real, no solo un `BUILD SUCCESS`. Si una estrategia falla repetidamente, diagnostica el problema y usa una alternativa razonable.

Conserva los cambios preexistentes del usuario. No hagas commit, push ni publiques cambios en GitHub sin autorización expresa del usuario.

## Entorno Java y Maven

- El proyecto declara Java 21 en `pom.xml`; utiliza JDK 21 para compilar y ejecutar cuando esté instalado. No cambies la versión declarada sin necesidad funcional.
- Comprueba `JAVA_HOME`, `java -version` y `mvn -version` al diagnosticar fallas. Si JDK 21 no está instalado, informa esa limitación y usa el JDK disponible solo después de confirmar que el proyecto compila y arranca con él.
- Mantén una única caché Maven en `%USERPROFILE%\.m2\repository`. No copies dependencias a `target/` ni crees cachés temporales dentro del proyecto.
- Si `user.home` de Java/Maven apunta a `C:\` en esta estación, pásale a Maven `-Duser.home=%USERPROFILE% -Dmaven.repo.local=%USERPROFILE%\.m2\repository`. Asegura permisos de lectura y escritura en la caché estándar antes de resolver dependencias.
- Cuando una dependencia falte, resuélvela en esa caché estándar; no sustituyas el repositorio local por copias temporales.
- Spring Boot 4 usa paquetes modulares. Mantén las importaciones de pruebas y APIs compatibles con las dependencias del `pom.xml`, y valida que las pruebas agregadas realmente se compilen antes de reportarlas como ejecutadas.
- El wrapper `mvnw.cmd` establece `user.home` y el repositorio Maven desde `%USERPROFILE%` en Windows. No añadas rutas personales al `pom.xml` ni a archivos Maven compartidos.
