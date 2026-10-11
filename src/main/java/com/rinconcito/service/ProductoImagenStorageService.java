package com.rinconcito.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ProductoImagenStorageService {

    private static final String RUTA_PUBLICA =
            "/productos/imagenes/";

    private final Path directorio;
    private final long maximoBytes;

    public ProductoImagenStorageService(
            @Value("${app.upload.productos-dir}")
            String directorio,

            @Value("${app.upload.productos-max-bytes:3145728}")
            long maximoBytes
    ) {
        this.directorio =
                Path.of(directorio)
                        .toAbsolutePath()
                        .normalize();

        this.maximoBytes = maximoBytes;
    }

    @PostConstruct
    public void inicializar() {
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de imágenes de productos.",
                    e
            );
        }
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalStateException(
                    "Debes seleccionar una imagen."
            );
        }

        if (archivo.getSize() > maximoBytes) {
            throw new IllegalStateException(
                    "La imagen no puede superar los 3 MB."
            );
        }

        String extension = detectarExtension(archivo);
        String nombreArchivo =
                UUID.randomUUID() + extension;

        Path destino =
                directorio.resolve(nombreArchivo)
                        .normalize();

        validarRuta(destino);

        try {
            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la imagen del producto.",
                    e
            );
        }

        return RUTA_PUBLICA + nombreArchivo;
    }

    public Path resolver(String nombreArchivo) {
        if (nombreArchivo == null
                || nombreArchivo.isBlank()) {
            throw new IllegalStateException(
                    "Nombre de imagen no válido."
            );
        }

        String nombreSeguro =
                Path.of(nombreArchivo)
                        .getFileName()
                        .toString();

        Path archivo =
                directorio.resolve(nombreSeguro)
                        .normalize();

        validarRuta(archivo);

        return archivo;
    }

    public void eliminar(String rutaPublica) {
        if (rutaPublica == null
                || rutaPublica.isBlank()) {
            return;
        }

        String nombreArchivo =
                Path.of(rutaPublica)
                        .getFileName()
                        .toString();

        Path archivo = resolver(nombreArchivo);

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar la imagen anterior.",
                    e
            );
        }
    }

    public String obtenerContentType(Path archivo) {
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
                        .toLowerCase();

        if (nombre.endsWith(".png")) {
            return "image/png";
        }

        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/jpeg";
    }

    private String detectarExtension(
            MultipartFile archivo
    ) {
        byte[] bytes;

        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer la imagen.",
                    e
            );
        }

        if (esJpeg(bytes)) {
            return ".jpg";
        }

        if (esPng(bytes)) {
            return ".png";
        }

        if (esWebp(bytes)) {
            return ".webp";
        }

        throw new IllegalStateException(
                "El archivo debe ser una imagen JPG, PNG o WEBP válida."
        );
    }

    private boolean esJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean esPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean esWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private void validarRuta(Path archivo) {
        if (!archivo.startsWith(directorio)) {
            throw new IllegalStateException(
                    "Ruta de imagen no permitida."
            );
        }
    }
}