package com.rinconcito.controller;

import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.service.ConfiguracionImagenStorageService;
import com.rinconcito.service.ConfiguracionRestauranteService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/configuracion/qr")
public class ConfiguracionImagenController {

    private final ConfiguracionRestauranteService
            configuracionRestauranteService;

    private final ConfiguracionImagenStorageService
            configuracionImagenStorageService;

    public ConfiguracionImagenController(
            ConfiguracionRestauranteService
                    configuracionRestauranteService,
            ConfiguracionImagenStorageService
                    configuracionImagenStorageService
    ) {
        this.configuracionRestauranteService =
                configuracionRestauranteService;

        this.configuracionImagenStorageService =
                configuracionImagenStorageService;
    }

    /*
     * Rutas públicas:
     *
     * /configuracion/qr/yape
     * /configuracion/qr/plin
     */
    @GetMapping("/{tipo}")
    public ResponseEntity<Resource> mostrarQr(
            @PathVariable String tipo
    ) {
        try {
            ConfiguracionRestaurante configuracion =
                    configuracionRestauranteService.obtener();

            if (configuracion == null) {
                return ResponseEntity.notFound().build();
            }

            String tipoNormalizado =
                    tipo.trim()
                            .toUpperCase(Locale.ROOT);

            String nombreArchivo;

            if ("YAPE".equals(tipoNormalizado)) {

                nombreArchivo =
                        configuracion.getQrYape();

            } else if ("PLIN".equals(tipoNormalizado)) {

                nombreArchivo =
                        configuracion.getQrPlin();

            } else {
                return ResponseEntity
                        .badRequest()
                        .build();
            }

            if (
                    nombreArchivo == null
                            || nombreArchivo.isBlank()
            ) {
                return ResponseEntity
                        .notFound()
                        .build();
            }

            Path archivo =
                    configuracionImagenStorageService
                            .resolver(nombreArchivo);

            Resource resource =
                    new UrlResource(
                            archivo.toUri()
                    );

            if (
                    !resource.exists()
                            || !resource.isReadable()
            ) {
                return ResponseEntity
                        .notFound()
                        .build();
            }

            String contentType =
                    configuracionImagenStorageService
                            .obtenerContentType(archivo);

            ContentDisposition disposicion =
                    ContentDisposition
                            .inline()
                            .filename(
                                    archivo.getFileName()
                                            .toString(),
                                    StandardCharsets.UTF_8
                            )
                            .build();

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposicion.toString()
                    )
                    .cacheControl(
                            CacheControl
                                    .maxAge(
                                            5,
                                            TimeUnit.MINUTES
                                    )
                                    .cachePublic()
                    )
                    .body(resource);

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .notFound()
                    .build();

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}