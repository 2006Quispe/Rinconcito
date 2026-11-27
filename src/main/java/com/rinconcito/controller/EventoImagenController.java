package com.rinconcito.controller;

import com.rinconcito.service.EventoImagenStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Controller
public class EventoImagenController {

    private final EventoImagenStorageService
            imagenStorageService;

    public EventoImagenController(
            EventoImagenStorageService imagenStorageService
    ) {
        this.imagenStorageService =
                imagenStorageService;
    }

    @GetMapping(
            "/eventos/imagenes/{nombreArchivo:.+}"
    )
    public ResponseEntity<Resource> mostrarImagen(
            @PathVariable String nombreArchivo
    ) {
        try {
            Path archivo =
                    imagenStorageService.resolver(
                            nombreArchivo
                    );

            Resource recurso =
                    new UrlResource(
                            archivo.toUri()
                    );

            if (
                    !recurso.exists()
                            || !recurso.isReadable()
            ) {
                return ResponseEntity
                        .notFound()
                        .build();
            }

            String contentType =
                    imagenStorageService
                            .obtenerContentType(archivo);

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .cacheControl(
                            CacheControl
                                    .maxAge(
                                            30,
                                            TimeUnit.DAYS
                                    )
                                    .cachePublic()
                    )
                    .body(recurso);

        } catch (Exception e) {
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
}