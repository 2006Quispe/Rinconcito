# Club Rinconcito

## Instalacion nueva para el grupo

Importa una sola vez el archivo `rinconcito_db.sql` de la raiz del proyecto en MySQL Workbench. Crea `rinconcito_db` con el esquema actual y datos ficticios de demostracion; no hace falta ejecutar despues las migraciones o semillas individuales.

El archivo es para una base vacia y no borra tablas existentes. Si `rinconcito_db` ya contiene tablas, importa el dump en otra instancia/esquema limpio o respalda y retira el esquema anterior antes de importar. Para actualizar una base existente conservando sus datos, usa el procedimiento de la siguiente seccion.

Incluye cuatro cuentas ficticias con correo `@rinconcito.test`; todas usan la clave de prueba `Prueba123!`. No reutilices esas cuentas fuera del entorno local de desarrollo.

## Actualizar una base existente

Abre `actualizar_rinconcito_db.sql` en MySQL Workbench y ejecútalo contra `rinconcito_db`. El script amplía la base existente, conserva los clientes, saldos, recompensas y movimientos, agrega las tablas de lotes, campañas y configuración, y se puede volver a ejecutar. No ejecutes los archivos históricos `migration_club_puntos.sql` y `migration_club_tienda.sql` para esta actualización.

Los saldos existentes se convierten en lotes sin fecha de vencimiento para no cambiar retroactivamente la vigencia. Los nuevos puntos vencen según la regla configurable (365 días al inicio); los canjes tienen vigencia propia (90 días al inicio).

## Rutas

- Cliente: `/club`
- Administración: `/admin/club` (rol `ADMINISTRADOR`)
- Caja: `/caja/club` (rol `CAJA`)

Caja también puede identificar al cliente con su DNI en el formulario de cobro presencial. Los puntos se acreditan únicamente al confirmar pagos aprobados y una sola vez por pago. Las recompensas de carta dependen de que el producto vinculado siga activo y disponible. Las experiencias familiares quedan ocultas hasta que Administración confirme disponibilidad, condiciones y stock.

La tarifa de delivery queda en S/ 0 mientras no se configure. Antes de ofrecer “Delivery gratis”, define `app.delivery.costo-soles` en `.local/application.properties` o la variable `DELIVERY_COST_SOLES` con el importe real; el Club mantendrá ese canje bloqueado hasta entonces.

## Ejecutar

Después de aplicar el SQL en tu Workbench, inicia la aplicación con `.\mvnw.cmd spring-boot:run` desde PowerShell y abre `http://localhost:8081`. La configuración local de MySQL se mantiene en `.local/application.properties`.

La suite automática usa H2 en memoria, no tu MySQL local.
