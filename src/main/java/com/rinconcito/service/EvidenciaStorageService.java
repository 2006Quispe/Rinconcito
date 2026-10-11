package com.rinconcito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Almacena capturas de pago (reservas y pedidos) fuera de static (privadas).
 * En MySQL solo se guarda el nombre relativo del archivo.
 * Mismo directorio y reglas para ambos flujos: JPG/JPEG/PNG/WEBP, max 5 MB.
 */
@Service
public class EvidenciaStorageService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg");

    private final Path rootDir;

    public EvidenciaStorageService(
            @Value("${app.upload.evidencias-pagos-dir:${app.upload.reservas-pagos-dir:${user.home}/rinconcito-uploads/evidencias-pagos}}")
            String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRootDir() {
        return rootDir;
    }

    /**
     * Valida y guarda el archivo. Devuelve nombre relativo (solo archivo) para persistir en MySQL.
     * @param codigoReferencia codigo de reserva (RES-...) o pedido (PED-...) usado en el nombre.
     */
    public String guardar(MultipartFile archivo, String codigoReferencia) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalStateException("Debes adjuntar la captura del pago.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalStateException("La captura no puede superar 5 MB.");
        }

        String original = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        String extension = extensionDe(original);
        if (!EXTENSIONES.contains(extension)) {
            throw new IllegalStateException("Solo se permiten imagenes JPG, JPEG, PNG o WEBP.");
        }

        String contentType = archivo.getContentType() != null
                ? archivo.getContentType().toLowerCase(Locale.ROOT)
                : "";
        if (!CONTENT_TYPES.contains(contentType)) {
            throw new IllegalStateException("El tipo de archivo no es una imagen valida (JPG/PNG/WEBP).");
        }

        try {
            Files.createDirectories(rootDir);
            String seguroCodigo = codigoReferencia == null
                    ? "PAGO"
                    : codigoReferencia.replaceAll("[^A-Za-z0-9\\-]", "_");
            String nombre = seguroCodigo + "_" + System.currentTimeMillis()
                    + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
            Path destino = rootDir.resolve(nombre).normalize();
            if (!destino.startsWith(rootDir)) {
                throw new IllegalStateException("Ruta de archivo invalida.");
            }
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return nombre;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la captura. Intenta de nuevo.");
        }
    }

    public Path resolverArchivo(String nombreRelativo) {
        if (nombreRelativo == null || nombreRelativo.isBlank()) {
            throw new IllegalStateException("No hay evidencia registrada.");
        }
        String limpio = Paths.get(nombreRelativo).getFileName().toString();
        Path archivo = rootDir.resolve(limpio).normalize();
        if (!archivo.startsWith(rootDir) || !Files.isRegularFile(archivo)) {
            throw new IllegalStateException("Evidencia no encontrada.");
        }
        return archivo;
    }

    public String contentTypeDe(Path archivo) {
        String name = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String extensionDe(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
