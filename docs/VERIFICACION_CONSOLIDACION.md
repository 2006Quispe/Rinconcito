# Verificación de consolidación

Fecha: 26 de noviembre de 2026.

## Instancia local

- Se detuvieron únicamente las instancias Java de Rinconcito que estaban en los puertos 8081 y 8082.
- Se empaquetó el proyecto con `.\mvnw.cmd -o -DskipTests package` y quedó una sola instancia actualizada en `http://localhost:8081`.
- MySQL permaneció activo; no se modificó durante esta consolidación.

## Comprobaciones de rutas

Se verificaron respuestas HTTP 200 en la aplicación iniciada con la configuración MySQL local:

- Público: `/pedidos`, `/reservas`.
- Cliente: `/mi-cuenta`, `/club`.
- Mozo: `/mesero/pedidos`, `/mesero/mesas`, `/mesero/reservas`, `/mesero/carta`.
- Caja: `/caja/dashboard`, `/caja/pedidos-presenciales`, `/caja/club`, `/caja/notas-venta`.
- Administración: `/admin/dashboard`, `/admin/pedidos`, `/admin/reservas`, `/admin/club`.

Estas son comprobaciones de carga y acceso con cuentas locales de demostración; no sustituyen una regresión completa de cada flujo de negocio.

## Nota de venta PDF

Se descargó y renderizó visualmente la nota de venta de demostración. La sección `CLUB RINCONCITO` aparece completa, sin recortes, e incluye el nivel al momento de la operación, saldo anterior, puntos base, bonificaciones, total acreditado y saldo histórico posterior al pago. La reimpresión conserva la instantánea histórica aunque el cliente haya canjeado puntos después.

## Base de datos compartible

El archivo raíz `rinconcito_db.sql` contiene las 28 tablas del esquema conectado. Se comparó su definición con el esquema actual de MySQL y coinciden las tablas, columnas y restricciones. Contiene datos ficticios de demostración y semillas de catálogo/configuración; excluye los movimientos, pagos, canjes y datos temporales creados para la verificación local, además de las credenciales privadas y la configuración `.local`.

El SQL incluye cuentas demo identificadas con dominio `.test` y una contraseña de prueba documentada dentro del propio archivo. Son credenciales ficticias para desarrollo; no reutilizarlas fuera de una instalación local de demostración.

## Imágenes del catálogo

Se comprobaron por HTTP 200 las diez imágenes WebP de recompensas bajo `src/main/resources/static/img/club/`, además de las imágenes existentes de pachamanca y cuy chactado. Git no las ignora. Las imágenes originales de Downloads se conservaron.

## Limitaciones pendientes

- JDK 21 no está instalado en esta estación; el empaquetado y la instancia se ejecutan con JDK 27. El proyecto declara Java 21 y conviene instalar esa versión para igualar el entorno recomendado.
- La prueba puntual de fidelización con H2 tuvo dos errores porque H2 no implementa sintaxis MySQL usada por consultas reales (`INSERT IGNORE` y `SUBSTRING_INDEX` con `CAST ... UNSIGNED`). Los otros dos casos de esa clase pasaron. No se ejecutó la suite completa.
- Las rutas se comprobaron mediante solicitudes HTTP autenticadas con cuentas locales; no se ejecutó una prueba automatizada completa de cada escenario de pedidos, reservas, caja, administración y mozo.
- `rinconcito_db.sql` es una instantánea compartible sin los movimientos personales de la base local. Los compañeros deberán importar ese archivo en MySQL y configurar sus propias credenciales locales en `.local/application.properties`.

## Preparación para GitHub

La entrega se limita al Club y sus integraciones necesarias sobre el `main` aprobado. La rama `reservas-pasarela` no formaba parte de `main` y no se incluye en esta publicación. `.local/`, respaldos locales, logs y artefactos de `target/` permanecen excluidos de Git.
