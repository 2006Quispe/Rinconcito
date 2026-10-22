# Apache POI en la exportación de reportes

## Qué cambió

- `pom.xml`: se agregó Apache POI `poi-ooxml` versión `5.4.1` como dependencia directa.
- `AdminReporteExcelService`: se creó este servicio para generar el archivo Excel (`.xlsx`) utilizando `XSSFWorkbook`, aplicando estilos de celda, formatos de moneda y anchos de columna.
- `AdminReporteController`: se agregó el endpoint `GET /admin/reportes/excel` que retorna el archivo generado con su tipo MIME correspondiente.
- `reportes.html`: se incluyó el botón "Descargar Excel" en la interfaz del administrador.

## Por qué se cambió

El sistema generaba reportes en formato HTML y PDF, pero no permitía a los administradores descargar la información en un formato de hoja de cálculo para su propio análisis o auditoría.

Apache POI permite construir archivos de Excel reales desde Java sin depender de que el servidor tenga instalado Microsoft Excel. Con la clase `XSSFWorkbook`, se genera el archivo en memoria y se estructuran los datos en diferentes hojas:

1. Resumen general.
2. Detalle de operaciones (con filtros automáticos).
3. Ventas diarias.
4. Resumen por método de pago.
5. Productos más vendidos.

El objetivo es proporcionar una herramienta de exportación profesional. Usar Apache POI permite aplicar formatos avanzados, como el símbolo de moneda (`S/ `), inmovilizar los paneles para que los encabezados siempre sean visibles y aplicar autofiltros. Aunque es posible generar archivos planos (CSV) con Java estándar, POI aporta la estructura y el diseño visual necesarios para un reporte gerencial.

## Comportamiento conservado

Se mantienen los filtros por fecha, las validaciones de seguridad (solo el rol `ADMINISTRADOR` puede acceder a `/admin/reportes/excel`), los cálculos de ventas y las consultas a la base de datos. La vista web y la exportación a PDF siguen funcionando exactamente igual. No se requieren cambios en las tablas de MySQL ni afecta los flujos de pedidos o reservas.

## Cómo usarlo en nuevo código

```java
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;

try (Workbook workbook = new XSSFWorkbook();
     ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

    // Crear una hoja
    var hoja = workbook.createSheet("Nueva Hoja");
    
    // Crear una fila y una celda
    var fila = hoja.createRow(0);
    fila.createCell(0).setCellValue("Hola Mundo");

    // Escribir el contenido al flujo de salida
    workbook.write(salida);
    
    return salida.toByteArray();

} catch (IOException e) {
    throw new IllegalStateException("No se pudo generar el Excel", e);
}

## Validación

Comando de validación:

```shell
./mvnw -Dtest=AdminReporteServiceTest,UsuarioServiceTest test
```

En Windows puede usarse mvnw.cmd o el Maven configurado en el IDE.

Resultado: Se verificó manualmente la descarga desde /admin/reportes/excel, comprobando que el archivo .xlsx se abre correctamente y contiene las 5 hojas con los datos, filtros, colores y montos formateados. Las 15 pruebas automatizadas existentes (seguridad y reportes) siguen aprobándose sin fallos.

Referencias: [Apache POI](https://poi.apache.org/), [XSSFWorkbook](https://poi.apache.org/apidocs/dev/org/apache/poi/xssf/usermodel/XSSFWorkbook.html).
