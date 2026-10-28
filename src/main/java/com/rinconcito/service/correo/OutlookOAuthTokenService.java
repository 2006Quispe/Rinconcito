package com.rinconcito.service.correo;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;

/** Renueva el consentimiento de una cuenta personal sin almacenar su contrasena. */
public class OutlookOAuthTokenService {
    static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private final Path archivo;
    private final String cuenta;
    private final RestClient cliente;
    private String token;
    private Instant vence = Instant.EPOCH;

    public OutlookOAuthTokenService(Path archivo, String cuenta, RestClient cliente) {
        this.archivo = archivo.toAbsolutePath().normalize();
        this.cuenta = cuenta;
        this.cliente = cliente;
        leerAutorizacion();
    }

    public synchronized String obtenerToken() {
        if (token != null && Instant.now().isBefore(vence)) return token;
        Properties autorizacion = leerAutorizacion();
        String formulario = "client_id=" + URLEncoder.encode(autorizacion.getProperty("client_id"), StandardCharsets.UTF_8)
                + "&scope=https%3A%2F%2Fgraph.microsoft.com%2FMail.Send%20offline_access&grant_type=refresh_token&refresh_token="
                + URLEncoder.encode(autorizacion.getProperty("refresh_token"), StandardCharsets.UTF_8);
        byte[] contenido = formulario.getBytes(StandardCharsets.UTF_8);
        Map<?, ?> respuesta;
        try {
            respuesta = cliente.post().uri(TOKEN_ENDPOINT).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    // Evita que los conversores HTTP impriman el refresh token con logging DEBUG.
                    .contentLength(contenido.length).body(salida -> salida.write(contenido))
                    .retrieve().body(Map.class);
        } catch (Exception error) {
            // No propagar cuerpos HTTP ni tokens a los logs del programa.
            throw new IllegalStateException("Microsoft no pudo renovar la autorizacion. Vuelve a autorizar Hotmail.");
        }
        if (respuesta == null || !(respuesta.get("access_token") instanceof String nuevoToken)
                || nuevoToken.isBlank() || !(respuesta.get("expires_in") instanceof Number duracion)
                || duracion.longValue() <= 60) {
            throw new IllegalStateException("Microsoft devolvio una autorizacion incompleta.");
        }
        if (respuesta.get("refresh_token") instanceof String renovacion && !renovacion.isBlank()) {
            autorizacion.setProperty("refresh_token", renovacion);
            guardarAutorizacion(autorizacion);
        }
        token = nuevoToken;
        vence = Instant.now().plusSeconds(duracion.longValue() - 60);
        return token;
    }

    public synchronized void invalidar() {
        token = null;
        vence = Instant.EPOCH;
    }

    private Properties leerAutorizacion() {
        var propiedades = new Properties();
        try (var reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(reader);
        } catch (Exception error) {
            throw new IllegalStateException("Falta la autorizacion de Hotmail. Ejecuta tools/autorizar_outlook.py.");
        }
        if (propiedades.getProperty("client_id", "").isBlank()
                || propiedades.getProperty("refresh_token", "").isBlank()
                || !cuenta.equalsIgnoreCase(propiedades.getProperty("username", ""))) {
            throw new IllegalStateException("La autorizacion no corresponde a la cuenta remitente configurada.");
        }
        return propiedades;
    }

    private void guardarAutorizacion(Properties propiedades) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile(archivo.getParent(), "outlook-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                propiedades.store(writer, "Autorizacion privada de Microsoft. No subir a Git.");
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo guardar la renovacion de Hotmail en el archivo privado.");
        } finally {
            if (temporal != null) try { Files.deleteIfExists(temporal); } catch (Exception ignored) { }
        }
    }
}
