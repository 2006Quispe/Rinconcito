# Carta y detalle de productos

Rama: `mejora-carta-detalle`.

## Actualización local

1. Detener la aplicación en Spring Tools.
2. Desde Git Bash, dentro del proyecto, comprobar la rama con `git branch --show-current`.
   Debe mostrar `mejora-carta-detalle`.
3. Ejecutar `git pull --ff-only origin mejora-carta-detalle`. Si Git avisa de cambios locales
   que se sobrescribirían, detenerse y revisarlos; no usar `reset --hard` ni borrar archivos.
4. Respaldar la base de datos. En MySQL Workbench, seleccionar la base de Rinconcito y ejecutar
   `src/main/resources/db/migration_producto_ingredientes.sql` **antes de iniciar la aplicación**.
   El script solo añade `productos.ingredientes` (VARCHAR de 1000 caracteres, opcional).
   No borra datos ni modifica los productos existentes. Se puede ejecutar de nuevo sin duplicar la columna.
5. En Spring Tools: seleccionar el proyecto, presionar F5 y ejecutar Project → Clean.
   Iniciar la aplicación y abrir `/carta`. Si se conservan los estilos anteriores, usar Ctrl+F5.

No se requieren tokens, nuevas dependencias ni cambios de `application.properties`.
Con `spring.jpa.hibernate.ddl-auto=validate`, omitir el paso SQL impide el inicio por la columna faltante.

## Comprobación funcional

- Abrir `/carta` sin iniciar sesión. Buscar, seleccionar una categoría y limpiar los filtros.
- Abrir un producto pulsando su foto o nombre. Comprobar foto, precio y descripción.
- Volver a la carta: conservar búsqueda, categoría y posición.
- Agregar un producto desde la carta: mostrar confirmación, actualizar subtotal y contador sin recargar.
- En el detalle, ajustar cantidad entre 1 y 30 y agregar. El límite por producto sigue siendo 30 unidades
  acumuladas; el servidor conserva la validación del carrito existente.
- Abrir el carrito y comprobar los productos y cantidades.
- Como administrador, crear o editar un producto: escribir ingredientes separados por comas o saltos de línea.
  Guardar y volver al detalle público: deben aparecer como etiquetas. Vaciar el campo los quita.
- Un producto sin ingredientes no muestra esa sección; no se inventan ingredientes para los productos existentes.
- Un producto agotado permite leer el detalle, pero no agregar. Un producto inactivo o de categoría inactiva
  no aparece en la carta y su detalle devuelve 404.
- Probar en un teléfono o ventana estrecha: detalle en una columna, categorías desplazables y controles legibles.

No se agregan observaciones ni controles 3D. No cambia el procesamiento de pedidos, pagos o correos.

## Ajuste del detalle y recomendaciones

- Foto horizontal 4:3 con altura máxima de 400 px y contenido centrado de hasta 1120 px en escritorio.
- “Acompaña tu mesa con:” muestra hasta tres productos aleatorios de toda la carta pública,
  sin repetir el producto abierto y excluyendo agotados, productos inactivos y categorías inactivas.
  Si hay menos de tres opciones, muestra las existentes; si no hay ninguna, oculta la sección.
- Cada tarjeta permite ver el detalle o agregar una unidad al carrito sin recargar.
- Las recomendaciones se seleccionan al cargar el detalle; agregar al carrito no las cambia.

Este ajuste no añade columnas ni requiere otro SQL. Si ya aplicaste la migración de ingredientes,
solo actualiza la rama, reinicia la aplicación y recarga con Ctrl+F5.

## Pruebas automatizadas de este cambio

En Git Bash, con Java 21 y acceso a las dependencias Maven:

```bash
bash mvnw -Dtest=ProductoIngredientesTest,ProductoPublicoRepositoryTest,ProductoRecomendacionesTest,CartaControllerTest,AdminCartaIngredientesTest,CartaVistaTest test
```

Estas pruebas usan dobles de los servicios y una base H2 temporal para comprobar la consulta pública.
No necesitan la base MySQL personal ni envían correos.
`CartaVistaTest` también procesa las plantillas Thymeleaf con seguridad y genera HTML de prueba en
`target/carta-vistas-prueba/`.

### Resultado de verificación de esta entrega

- Compilación y 20 pruebas específicas: correctas (Java 21).
- Suite completa: 94 pruebas; conserva los mismos cuatro casos que fallaban antes del cambio
  (la versión base tenía 74 pruebas y esos mismos cuatro casos).
  Tres pertenecen a `AdminReporteControllerTest`, cuyo contexto de prueba no proporciona
  `UserDetailsService`. El otro pertenece a `RegistroFormDTOValidationTest.rechazaCorreoInvalido`.
  No se modificaron esos módulos ni sus pruebas.
- Sintaxis de JavaScript y revisión de espacios/conflictos con `git diff --check`: correctas.
- La revisión visual e interacción en navegador no pudo ejecutarse por restricciones del entorno.
  Aplicar la lista de comprobación funcional después de actualizar la base local.
