# SOLID en la exportaci?n de reportes

El m?dulo conserva los c?lculos de ventas y los documentos PDF/Excel existentes,
pero comparte la coordinaci?n de las descargas. No incorpora nuevas dependencias.

## Responsabilidades

```text
AdminReporteController ? AdminReporteService ? repositorios
                      ? ExportacionReporteService ? ExportadorReporte
                                                     ?? AdminReportePdfService
                                                     ?? AdminReporteExcelService
```

El controlador consulta el reporte una vez y prepara la respuesta HTTP.
`ExportacionReporteService` selecciona el exportador y construye el nombre usando
las fechas normalizadas del DTO. Los generadores conservan PDFBox y POI.
El PDF sigue consultando la configuraci?n del restaurante para su encabezado.

## Cuatro principios aplicados

| Principio | Aplicaci?n y evidencia |
| --- | --- |
| SRP | HTTP, coordinaci?n, c?lculo y representaci?n tienen componentes separados. Cambiar hojas de Excel no modifica el controlador. |
| OCP | El coordinador recibe `List<ExportadorReporte>` y registra estrategias por identificador, sin condicionales por formato. Una prueba a?ade otra estrategia sin editarlo. |
| LSP | PDF y Excel aceptan el mismo DTO v?lido, incluso sin operaciones, conservan los datos y devuelven documentos legibles. Las pruebas usan ambas implementaciones mediante la interfaz. |
| DIP | El coordinador depende de `ExportadorReporte`, no de los generadores concretos ni de sus bibliotecas. Puede probarse con una implementaci?n falsa. |

El contrato exige metadatos coherentes, contenido no vac?o y no modificar el
reporte. Un fallo de generaci?n produce una excepci?n. No se exige que PDF y Excel
tengan el mismo dise?o o todas las mismas secciones.

DIP se aplica a la frontera entre coordinaci?n y exportadores; el controlador
conserva sus servicios concretos. No se cuenta ISP ?nicamente por tener una
interfaz peque?a ni se exige una interfaz para cada servicio.

## Uso y extensi?n

Se conservan los botones y rutas `/admin/reportes/pdf` y `/admin/reportes/excel`.
Tambi?n est? disponible:

```text
/admin/reportes/exportar?formato=excel&desde=2026-10-01&hasta=2026-10-22
```

Los identificadores son `pdf` y `excel`; sus extensiones son `pdf` y `xlsx`.
Sin fechas se usa el periodo predeterminado del servicio de reportes. Las tres
rutas siguen protegidas por el rol `ADMINISTRADOR` en `SecurityConfig`.

Para a?adir un formato, crear un servicio Spring que implemente `ExportadorReporte`
con identificador ?nico y usar la ruta gen?rica. Solo ser?a necesario editar HTML
si se desea agregar un bot?n. No hace falta a?adir otro formato para la entrega.

Un formato desconocido o un periodo inv?lido devuelve 400; un fallo al generar
el documento devuelve 500 y queda registrado. Los identificadores duplicados
impiden crear el coordinador. La excepci?n espec?fica de formato evita confundir
ese error de solicitud con los fallos t?cnicos de PDF/Excel.

## Comprobaci?n

Ejecutar con Java compatible con el `pom.xml` (Java 21 en el repositorio):

```powershell
.\mvnw.cmd "-Dtest=AdminReporteControllerTest,ExportacionReporteServiceTest,ExportadoresReporteTest,AdminReporteServiceTest,UsuarioServiceTest,LogbackConfiguracionTest" test
```

Las pruebas comprueban selecci?n/extensi?n de estrategias, registros inv?lidos,
contenido vac?o, nombres y MIME; abren PDF y Excel para revisar periodo y ventas
con y sin operaciones; verifican rutas, cabeceras, errores y permisos usando la
configuraci?n de seguridad real y servicios simulados. Las regresiones existentes
cubren reportes, usuarios y logging. No requieren MySQL ni datos personales reales.

Para sustentar: mostrar la interfaz y sus dos implementaciones, el coordinador,
la prueba de estrategia adicional y los documentos. Como comprobaci?n manual
adicional, descargar ambos desde la pantalla con los mismos filtros y comparar
importes sin cambios concurrentes de ventas. Esa comprobaci?n con MySQL/navegador
no forma parte de las pruebas automatizadas.
