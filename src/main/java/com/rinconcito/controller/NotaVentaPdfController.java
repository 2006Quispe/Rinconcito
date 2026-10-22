package com.rinconcito.controller;

import com.rinconcito.entity.NotaVenta;
import com.rinconcito.service.NotaVentaPdfService;
import com.rinconcito.service.NotaVentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/caja/notas-venta")
public class NotaVentaPdfController {

    private static final Logger log = LoggerFactory.getLogger(NotaVentaPdfController.class);

    private final NotaVentaPdfService notaVentaPdfService;
    private final NotaVentaService notaVentaService;

    public NotaVentaPdfController(
            NotaVentaPdfService notaVentaPdfService,
            NotaVentaService notaVentaService
    ) {
        this.notaVentaPdfService = notaVentaPdfService;
        this.notaVentaService = notaVentaService;
    }

    @GetMapping("/{idNota}/pdf")
    public ResponseEntity<byte[]> obtenerPdf(
            @PathVariable Integer idNota,
            @RequestParam(
                    name = "descargar",
                    defaultValue = "false"
            ) boolean descargar
    ) {
        // Este detalle solo hace falta cuando estamos investigando un problema.
        log.debug("Generando PDF de la nota {}", idNota);

        try {
            NotaVenta notaVenta =
                    notaVentaService.obtenerParaPdf(idNota);

            byte[] contenidoPdf =
                    notaVentaPdfService.generar(idNota);

            log.info("PDF generado para la nota {} ({} bytes)", idNota, contenidoPdf.length);

            String numeroNota =
                    notaVentaService.formarNumero(notaVenta);

            String nombreArchivo =
                    "NotaVenta-" + numeroNota + ".pdf";

            ContentDisposition disposicion;

            if (descargar) {
                disposicion = ContentDisposition
                        .attachment()
                        .filename(
                                nombreArchivo,
                                StandardCharsets.UTF_8
                        )
                        .build();
            } else {
                disposicion = ContentDisposition
                        .inline()
                        .filename(
                                nombreArchivo,
                                StandardCharsets.UTF_8
                        )
                        .build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposicion.toString()
                    )
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store, no-cache, must-revalidate"
                    )
                    .contentLength(contenidoPdf.length)
                    .body(contenidoPdf);

        } catch (Exception e) {
            log.error("No se pudo generar el PDF de la nota {}", idNota, e);

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}
