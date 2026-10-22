# Apache Commons Lang3 en las validaciones de texto

## Qué cambió

- `pom.xml`: se agregó Apache Commons Lang3 `3.18.0` como dependencia directa.
- `ReservaService`: `blankToNull` ahora delega en `StringUtils.trimToNull`, y las validaciones de nombres y apellidos usan `StringUtils.isBlank`.
- `AdminPedidoService`: `normalizarBusqueda` usa `StringUtils.trimToNull`.
- `ProductoService`: la normalización del texto de búsqueda en `buscarPublicos` usa `StringUtils.trimToNull`.

## Por qué se cambió

El código repetía en muchos puntos la misma comprobación manual: que el texto no sea `null`, que no esté vacío o en blanco y, si es válido, quitarle los espacios sobrantes. Por ejemplo:

```java
String busqueda = q == null || q.isBlank() ? null : q.trim();
```

`StringUtils.trimToNull` expresa esa intención en una sola llamada:

```java
String busqueda = StringUtils.trimToNull(q);
```

Si el texto es `null`, está vacío o solo contiene espacios, devuelve `null`. En otro caso devuelve el texto sin espacios al inicio ni al final. Es el mismo resultado que la versión manual, con menos código y menos lugares donde equivocarse.

`StringUtils.isBlank` hace lo mismo para las validaciones: reemplaza `x == null || x.isBlank()` por una sola llamada que además no lanza `NullPointerException`.

El objetivo es reducir código repetido y hacer más legibles las validaciones, no mejorar el rendimiento. Java permite hacer lo mismo con métodos estándar, por lo que la librería aporta claridad y no una capacidad nueva.

## Comportamiento conservado

Se mantienen los mensajes de error, las reglas de negocio, los permisos y las respuestas HTTP. Los valores `null`, vacíos y con solo espacios se tratan igual que antes. No se requieren cambios en tablas ni en las vistas.

## Cómo usarlo en nuevo código

```java
import org.apache.commons.lang3.StringUtils;

StringUtils.isBlank(texto);       // true si es null, vacío o solo espacios
StringUtils.trimToNull(texto);    // texto sin espacios, o null si queda vacío
StringUtils.isNotBlank(texto);    // lo contrario de isBlank
```

Hay otros usos pendientes en el proyecto con el mismo patrón (por ejemplo, en `ConfiguracionImagenStorageService`, `NotaVentaPdfService` y `EvidenciaStorageService`) que pueden migrarse siguiendo el mismo criterio.

## Validación

Comando de validación:

```shell
./mvnw -Dtest=AdminReporteServiceTest,UsuarioServiceTest test
```

En Windows puede usarse `mvnw.cmd` o el Maven configurado en el IDE.

Resultado: 15 pruebas aprobadas, cero fallos y cero errores.

Referencias: [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/), [StringUtils](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html).
