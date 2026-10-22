package com.rinconcito.controller;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteDiaDTO;
import com.rinconcito.dto.ReporteMetodoPagoDTO;
import com.rinconcito.service.AdminReporteExcelService;
import com.rinconcito.service.AdminReportePdfService;
import com.rinconcito.service.AdminReporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    private static final Logger log = LoggerFactory.getLogger(AdminReporteController.class);

    private final AdminReporteService
            adminReporteService;

    private final AdminReportePdfService
            adminReportePdfService;

    private final AdminReporteExcelService
            adminReporteExcelService;

    public AdminReporteController(
            AdminReporteService adminReporteService,
            AdminReportePdfService adminReportePdfService,
            AdminReporteExcelService adminReporteExcelService
    ) {
        this.adminReporteService =
                adminReporteService;

        this.adminReportePdfService =
                adminReportePdfService;

        this.adminReporteExcelService =
                adminReporteExcelService;
    }

    @GetMapping
    public String mostrarReporte(
            @RequestParam(
                    value = "desde",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(
                    value = "hasta",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta,

            Model model
    ) {
        AdminReporteDTO reporte;

        try {
            reporte =
                    adminReporteService.generar(
                            desde,
                            hasta
                    );

        } catch (IllegalStateException e) {
            log.warn("No se pudo consultar el reporte del periodo {} a {}", desde, hasta);
            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            reporte =
                    adminReporteService.generar(
                            null,
                            null
                    );
        }

        model.addAttribute(
                "titulo",
                "Reportes"
        );

        model.addAttribute(
                "reporte",
                reporte
        );

        model.addAttribute(
                "filtroDesde",
                reporte.getDesde()
        );

        model.addAttribute(
                "filtroHasta",
                reporte.getHasta()
        );

        prepararDatosGraficos(
                model,
                reporte
        );

        return "paginas/admin/reportes";
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(
                    value = "desde",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(
                    value = "hasta",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta
    ) {
        try {
            AdminReporteDTO reporte =
                    adminReporteService.generar(
                            desde,
                            hasta
                    );

            byte[] contenido =
                    adminReportePdfService.generar(
                            reporte
                    );

            log.info("Reporte PDF generado del {} al {} ({} bytes)",
                    reporte.getDesde(), reporte.getHasta(), contenido.length);

            String nombreArchivo =
                    construirNombreArchivo(
                            reporte,
                            "pdf"
                    );

            ContentDisposition disposicion =
                    ContentDisposition
                            .attachment()
                            .filename(
                                    nombreArchivo,
                                    StandardCharsets.UTF_8
                            )
                            .build();

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposicion.toString()
                    )
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store, no-cache, must-revalidate"
                    )
                    .contentLength(
                            contenido.length
                    )
                    .body(contenido);

        } catch (IllegalStateException e) {
            log.warn("No se pudo exportar el reporte PDF del periodo {} a {}", desde, hasta, e);
            return ResponseEntity
                    .badRequest()
                    .build();

        } catch (Exception e) {
            log.error("Error inesperado al exportar el reporte PDF del periodo {} a {}", desde, hasta, e);
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(
                    value = "desde",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(
                    value = "hasta",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta
    ) {
        try {
            AdminReporteDTO reporte =
                    adminReporteService.generar(
                            desde,
                            hasta
                    );

            byte[] contenido =
                    adminReporteExcelService.generar(
                            reporte
                    );

            log.info("Reporte Excel generado del {} al {} ({} bytes)",
                    reporte.getDesde(), reporte.getHasta(), contenido.length);

            String nombreArchivo =
                    construirNombreArchivo(
                            reporte,
                            "xlsx"
                    );

            ContentDisposition disposicion =
                    ContentDisposition
                            .attachment()
                            .filename(
                                    nombreArchivo,
                                    StandardCharsets.UTF_8
                            )
                            .build();

            MediaType tipoExcel =
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-"
                                    + "officedocument.spreadsheetml.sheet"
                    );

            return ResponseEntity
                    .ok()
                    .contentType(tipoExcel)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposicion.toString()
                    )
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store, no-cache, must-revalidate"
                    )
                    .contentLength(
                            contenido.length
                    )
                    .body(contenido);

        } catch (IllegalStateException e) {
            log.warn("No se pudo exportar el reporte Excel del periodo {} a {}", desde, hasta, e);
            return ResponseEntity
                    .badRequest()
                    .build();

        } catch (Exception e) {
            log.error("Error inesperado al exportar el reporte Excel del periodo {} a {}", desde, hasta, e);
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    private void prepararDatosGraficos(
            Model model,
            AdminReporteDTO reporte
    ) {
        List<String> etiquetasVentas =
                reporte.getVentasDiarias()
                        .stream()
                        .map(
                                ReporteDiaDTO::getEtiqueta
                        )
                        .toList();

        List<Long> montosVentas =
                reporte.getVentasDiarias()
                        .stream()
                        .map(
                                ReporteDiaDTO::getMonto
                        )
                        .toList();

        List<String> etiquetasMetodos =
                reporte.getMetodosPago()
                        .stream()
                        .map(
                                ReporteMetodoPagoDTO::getMetodo
                        )
                        .toList();

        List<Long> montosMetodos =
                reporte.getMetodosPago()
                        .stream()
                        .map(
                                ReporteMetodoPagoDTO::getMonto
                        )
                        .toList();

        List<Long> cantidadesMetodos =
                reporte.getMetodosPago()
                        .stream()
                        .map(
                                ReporteMetodoPagoDTO::getCantidad
                        )
                        .toList();

        model.addAttribute(
                "etiquetasVentas",
                etiquetasVentas
        );

        model.addAttribute(
                "montosVentas",
                montosVentas
        );

        model.addAttribute(
                "etiquetasMetodos",
                etiquetasMetodos
        );

        model.addAttribute(
                "montosMetodos",
                montosMetodos
        );

        model.addAttribute(
                "cantidadesMetodos",
                cantidadesMetodos
        );
    }

    private String construirNombreArchivo(
            AdminReporteDTO reporte,
            String extension
    ) {
        return "Reporte-Rinconcito-"
                + reporte.getDesde()
                + "-al-"
                + reporte.getHasta()
                + "."
                + extension;
    }
}
