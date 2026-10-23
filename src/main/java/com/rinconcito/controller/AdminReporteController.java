package com.rinconcito.controller;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteDiaDTO;
import com.rinconcito.dto.ReporteMetodoPagoDTO;
import com.rinconcito.dto.ArchivoReporteDTO;
import com.rinconcito.service.ExportacionReporteService;
import com.rinconcito.service.reporte.FormatoReporteNoSoportadoException;
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

    private final AdminReporteService adminReporteService;
    private final ExportacionReporteService exportacionReporteService;

    public AdminReporteController(AdminReporteService adminReporteService,
                                  ExportacionReporteService exportacionReporteService) {
        this.adminReporteService = adminReporteService;
        this.exportacionReporteService = exportacionReporteService;
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
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("pdf", desde, hasta);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar("excel", desde, hasta);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam("formato") String formato,
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descargar(formato, desde, hasta);
    }

    private ResponseEntity<byte[]> descargar(String formato, LocalDate desde, LocalDate hasta) {
        AdminReporteDTO reporte;
        try {
            reporte = adminReporteService.generar(desde, hasta);
        } catch (IllegalStateException e) {
            log.warn("Periodo de reporte no valido: {} a {}", desde, hasta);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error al consultar el reporte", e);
            return ResponseEntity.internalServerError().build();
        }

        try {
            ArchivoReporteDTO archivo = exportacionReporteService.exportar(reporte, formato);
            ContentDisposition disposicion = ContentDisposition.attachment()
                    .filename(archivo.nombreArchivo(), StandardCharsets.UTF_8).build();
            log.info("Reporte generado del {} al {} ({} bytes)",
                    reporte.getDesde(), reporte.getHasta(), archivo.contenido().length);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .contentLength(archivo.contenido().length)
                    .body(archivo.contenido());
        } catch (FormatoReporteNoSoportadoException e) {
            log.warn("Se solicito un formato de reporte no soportado");
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error al generar el documento del reporte", e);
            return ResponseEntity.internalServerError().build();
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

}
