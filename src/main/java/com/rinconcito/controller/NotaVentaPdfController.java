package com.rinconcito.controller;

import com.rinconcito.entity.NotaVenta;
import com.rinconcito.service.NotaVentaPdfService;
import com.rinconcito.service.NotaVentaService;
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
        System.out.println(
                "=== INICIANDO PDF DE NOTA " + idNota + " ==="
        );

        try {
            NotaVenta notaVenta =
                    notaVentaService.obtenerParaPdf(idNota);

            System.out.println(
                    "Nota encontrada: "
                            + notaVentaService.formarNumero(notaVenta)
            );

            byte[] contenidoPdf =
                    notaVentaPdfService.generar(idNota);

            System.out.println(
                    "PDF generado correctamente. Bytes: "
                            + contenidoPdf.length
            );

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

        } catch (Throwable e) {
            System.err.println(
                    "=== ERROR GENERANDO PDF DE NOTA "
                            + idNota
                            + " ==="
            );

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}