package com.rinconcito.service;

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
public class ConfiguracionImagenStorageService {

    private static final long TAMANO_MAXIMO =
            3L * 1024L * 1024L;

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final Path directorioConfiguracion =
            Path.of(
                    "uploads",
                    "configuracion"
            )
            .toAbsolutePath()
            .normalize();

    public ConfiguracionImagenStorageService() {
        try {
            Files.createDirectories(
                    directorioConfiguracion
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio "
                            + "para las imágenes de configuración.",
                    e
            );
        }
    }

    /*
     * Guarda un nuevo código QR.
     *
     * tipo:
     * YAPE
     * PLIN
     */
    public String guardarQr(
            MultipartFile archivo,
            String tipo
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalStateException(
                    "Debes seleccionar una imagen."
            );
        }

        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalStateException(
                    "La imagen no puede superar los 3 MB."
            );
        }

        String contentType =
                archivo.getContentType();

        if (
                contentType == null
                        || !TIPOS_PERMITIDOS.contains(
                                contentType.toLowerCase(
                                        Locale.ROOT
                                )
                        )
        ) {
            throw new IllegalStateException(
                    "Solo se permiten imágenes JPG, PNG o WEBP."
            );
        }

        String tipoNormalizado =
                normalizarTipo(tipo);

        String extension =
                obtenerExtension(contentType);

        String nombreArchivo =
                "qr-"
                        + tipoNormalizado.toLowerCase(
                                Locale.ROOT
                        )
                        + "-"
                        + UUID.randomUUID()
                        + extension;

        Path destino =
                directorioConfiguracion
                        .resolve(nombreArchivo)
                        .normalize();

        validarRuta(destino);

        try {
            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return nombreArchivo;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la imagen QR.",
                    e
            );
        }
    }

    /*
     * Obtiene la ruta física de una imagen guardada.
     */
    public Path resolver(String nombreArchivo) {

        if (
                nombreArchivo == null
                        || nombreArchivo.isBlank()
        ) {
            throw new IllegalStateException(
                    "La imagen no está registrada."
            );
        }

        /*
         * Por compatibilidad, si anteriormente se guardó
         * una ruta completa, conserva solo el nombre.
         */
        String nombreSeguro =
                Path.of(nombreArchivo)
                        .getFileName()
                        .toString();

        Path archivo =
                directorioConfiguracion
                        .resolve(nombreSeguro)
                        .normalize();

        validarRuta(archivo);

        return archivo;
    }

    /*
     * Elimina una imagen anterior después de reemplazarla.
     */
    public void eliminarSiExiste(String nombreArchivo) {

        if (
                nombreArchivo == null
                        || nombreArchivo.isBlank()
        ) {
            return;
        }

        try {
            Path archivo =
                    resolver(nombreArchivo);

            Files.deleteIfExists(archivo);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar la imagen anterior.",
                    e
            );
        }
    }

    /*
     * Retorna el tipo de contenido para mostrar la imagen.
     */
    public String obtenerContentType(Path archivo) {

        if (archivo == null) {
            return "application/octet-stream";
        }

        try {
            String contentType =
                    Files.probeContentType(archivo);

            if (contentType != null) {
                return contentType;
            }

        } catch (IOException ignored) {
        }

        String nombre =
                archivo.getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);

        if (
                nombre.endsWith(".jpg")
                        || nombre.endsWith(".jpeg")
        ) {
            return "image/jpeg";
        }

        if (nombre.endsWith(".png")) {
            return "image/png";
        }

        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }

        return "application/octet-stream";
    }

    private String normalizarTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalStateException(
                    "El tipo de código QR es obligatorio."
            );
        }

        String tipoNormalizado =
                tipo.trim()
                        .toUpperCase(Locale.ROOT);

        if (
                !"YAPE".equals(tipoNormalizado)
                        && !"PLIN".equals(tipoNormalizado)
        ) {
            throw new IllegalStateException(
                    "El tipo de código QR no es válido."
            );
        }

        return tipoNormalizado;
    }

    private String obtenerExtension(
            String contentType
    ) {
        return switch (
                contentType.toLowerCase(Locale.ROOT)
        ) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";

            default -> throw new IllegalStateException(
                    "El formato de imagen no es válido."
            );
        };
    }

    private void validarRuta(Path archivo) {

        if (
                archivo == null
                        || !archivo.startsWith(
                                directorioConfiguracion
                        )
        ) {
            throw new IllegalStateException(
                    "La ruta de la imagen no es válida."
            );
        }
    }
}