package com.rinconcito.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GaleriaImagenStorageService {

    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of(
                    "jpg",
                    "jpeg",
                    "png",
                    "webp"
            );

    private final Path directorioGaleria;
    private final long maximoBytes;

    public GaleriaImagenStorageService(
            @Value("${app.upload.galeria-dir}")
            String directorio,

            @Value("${app.upload.galeria-max-bytes:5242880}")
            long maximoBytes
    ) {
        this.directorioGaleria =
                Path.of(directorio)
                        .toAbsolutePath()
                        .normalize();

        this.maximoBytes = maximoBytes;
    }

    @PostConstruct
    public void prepararDirectorio() {
        try {
            Files.createDirectories(
                    directorioGaleria
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de la galería.",
                    e
            );
        }
    }

    public String guardar(
            MultipartFile archivo
    ) {
        validarArchivo(archivo);

        String extension =
                obtenerExtension(
                        archivo.getOriginalFilename()
                );

        String nombreArchivo =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        + "."
                        + extension;

        Path destino =
                directorioGaleria
                        .resolve(nombreArchivo)
                        .normalize();

        if (!destino.startsWith(directorioGaleria)) {
            throw new IllegalStateException(
                    "La ruta de la imagen no es válida."
            );
        }

        try {
            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la imagen de la galería.",
                    e
            );
        }

        return "/galeria/imagenes/" + nombreArchivo;
    }

    public Path resolver(
            String nombreArchivo
    ) {
        if (
                nombreArchivo == null
                        || nombreArchivo.isBlank()
        ) {
            throw new IllegalStateException(
                    "El nombre de la imagen es obligatorio."
            );
        }

        String nombreSeguro =
                Path.of(nombreArchivo)
                        .getFileName()
                        .toString();

        if (!nombreSeguro.equals(nombreArchivo)) {
            throw new IllegalStateException(
                    "El nombre de la imagen no es válido."
            );
        }

        Path archivo =
                directorioGaleria
                        .resolve(nombreSeguro)
                        .normalize();

        if (!archivo.startsWith(directorioGaleria)) {
            throw new IllegalStateException(
                    "La ruta de la imagen no es válida."
            );
        }

        return archivo;
    }

    public void eliminarPorRuta(
            String rutaImagen
    ) {
        if (
                rutaImagen == null
                        || rutaImagen.isBlank()
                        || !rutaImagen.startsWith(
                                "/galeria/imagenes/"
                        )
        ) {
            return;
        }

        String nombreArchivo =
                rutaImagen.substring(
                        rutaImagen.lastIndexOf('/') + 1
                );

        try {
            Files.deleteIfExists(
                    resolver(nombreArchivo)
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar la imagen anterior.",
                    e
            );
        }
    }

    public String obtenerContentType(
            Path archivo
    ) {
        try {
            String contentType =
                    Files.probeContentType(archivo);

            if (
                    contentType == null
                            || !contentType.startsWith("image/")
            ) {
                return "application/octet-stream";
            }

            return contentType;

        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private void validarArchivo(
            MultipartFile archivo
    ) {
        if (
                archivo == null
                        || archivo.isEmpty()
        ) {
            throw new IllegalStateException(
                    "Debes seleccionar una imagen."
            );
        }

        if (archivo.getSize() > maximoBytes) {
            throw new IllegalStateException(
                    "La imagen no puede superar los 5 MB."
            );
        }

        String extension =
                obtenerExtension(
                        archivo.getOriginalFilename()
                );

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalStateException(
                    "Solo se permiten imágenes JPG, JPEG, PNG o WEBP."
            );
        }

        String contentType =
                archivo.getContentType();

        if (
                contentType == null
                        || !contentType.startsWith("image/")
        ) {
            throw new IllegalStateException(
                    "El archivo seleccionado no es una imagen válida."
            );
        }
    }

    private String obtenerExtension(
            String nombreOriginal
    ) {
        if (
                nombreOriginal == null
                        || nombreOriginal.isBlank()
                        || !nombreOriginal.contains(".")
        ) {
            throw new IllegalStateException(
                    "La imagen no tiene una extensión válida."
            );
        }

        return nombreOriginal
                .substring(
                        nombreOriginal.lastIndexOf('.') + 1
                )
                .toLowerCase(Locale.ROOT);
    }
}