# Base demo para capturas del mozo

`rinconcito_demo_mozo.sql` crea una base **separada**, llamada
`rinconcito_demo_mozo`. No elimina ni modifica `rinconcito_db`.
Los datos y las identidades son ficticios; no son un respaldo de otra persona.

## Instalacion

Sigue [la guia de ejecucion](../../../../../docs/EJECUTAR_EN_OTRA_PC.md).
Importa `rinconcito_demo_mozo.sql` UNA SOLA VEZ en MySQL Workbench con permiso
para crear bases. Incluye el historial de disponibilidad y la cola de correo.
No necesita un usuario MySQL llamado rinconcito_app: usa las credenciales locales
de tu equipo en `.local/application.properties`.

Si la base ya existe, no repitas el archivo: no contiene DROP ni TRUNCATE.
Para correo en una base existente basta aplicar `migration_correo_salida.sql`
sobre esa base. El nombre de la conexion MySQL no es una cuenta web del sistema.

## Cuentas web

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Mozo | `mesero@rinconcito.pe` | `Prueba123!` |
| Administrador | `admin@rinconcito.pe` | `Prueba123!` |
| Caja | `caja@rinconcito.pe` | `Prueba123!` |

Las cuentas de clientes ficticios también usan esa clave, pero no son necesarias
para capturar el módulo del mozo.

## Datos incluidos

- 19 tablas (17 del sistema, historial de disponibilidad y cola de correo) con datos relacionados: 8 categorías, 20 productos y 3 pisos con 12 mesas.
- 19 productos disponibles y uno agotado para mostrar ambos estados.
- 4 reservas operativas del día: confirmadas, cliente llegó y en atención;
  además, una cancelada y una finalizada de ayer.
- 4 pedidos activos: dos confirmados, uno esperando cobro y otro esperando
  validación; un pedido finalizado y otro cancelado para el historial del mozo.
- Detalles, comandas, pagos de ejemplo y notas de venta con importes coherentes.
- Horarios, configuración, eventos, direcciones y galería de demostración.

Los tres platos con fotografías usan imágenes que ya incluye el proyecto.
Los demás productos usan la presentación sin imagen de la aplicación; no
dependen de enlaces de fotografías externos. Los datos bancarios son ficticios.
El horario amplio es solo para facilitar pruebas, no un horario comercial real.

## Recorrido para las capturas

| Pantalla | Qué usar |
| --- | --- |
| Control de mesas | Ya muestra disponibles, ocupadas, reservadas y una inactiva. |
| Nuevo pedido | Selecciona la **mesa 7**, ocupada y sin pedido activo; añade productos y captura antes de guardar. También puedes ocupar la mesa 4 para otro ejemplo. |
| Pedidos activos | Los pedidos `PED-DEMO-001`, `002`, `003` y `006` muestran los distintos estados. |
| Agregar productos | Abre esa acción en `PED-DEMO-001`, selecciona un plato y captura. |
| Comanda | Abre Ver comanda en `PED-DEMO-001`: ya contiene platos, cantidades y observación. |
| Reservas del día | Ya hay cuatro reservas visibles y clientes ficticios con sus estados. |
| Historial | Incluye `PED-DEMO-004` finalizado y `PED-DEMO-005` cancelado, asignados al usuario del mozo. |

Las fechas se calculan al importar, con zona horaria de Perú. Si tomas las
capturas otro día o las reservas quedan fuera de su horario, ejecuta
`actualizar_fechas_demo.sql`. Esto mueve las reservas aún operativas al día y
horario actuales, sin reiniciar estados ni borrar lo que probaste.
