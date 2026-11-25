package com.rinconcito.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class UbicacionEntregaService {
    private final RestClient cliente;
    private final String clave;

    public UbicacionEntregaService(@Value("${app.delivery.geocoding-key:${GOOGLE_MAPS_GEOCODING_KEY:}}") String clave) {
        this.clave = clave;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        var solicitudes = new JdkClientHttpRequestFactory(http);
        solicitudes.setReadTimeout(Duration.ofSeconds(5));
        cliente = RestClient.builder().requestFactory(solicitudes)
                .baseUrl("https://maps.googleapis.com").build();
    }

    public record Ubicacion(String direccion, String distrito, double latitud, double longitud) {}

    public Ubicacion buscar(Double latitud, Double longitud, String direccion, String distrito) {
        boolean inversa = latitud != null || longitud != null;
        if (inversa && (!valida(latitud, 90) || !valida(longitud, 180))) {
            throw new IllegalStateException("La ubicación recibida no es válida.");
        }
        if (!inversa && (direccion == null || direccion.isBlank() || direccion.length() > 200
                || distrito == null || distrito.isBlank() || distrito.length() > 50)) {
            throw new IllegalStateException("Completa la dirección y el distrito.");
        }
        if (clave.isBlank()) {
            throw new IllegalStateException("La búsqueda de direcciones todavía no está configurada.");
        }

        Map<?, ?> respuesta;
        try {
            respuesta = cliente.get().uri(b -> {
                var url = b.path("/maps/api/geocode/json").queryParam("key", clave).queryParam("language", "es");
                if (inversa) {
                    url.queryParam("latlng", latitud + "," + longitud);
                } else {
                    url.queryParam("address", "{direccion}").queryParam("components", "country:PE");
                    return url.build(direccion + ", " + distrito + ", Lima, Perú");
                }
                return url.build();
            }).retrieve().body(Map.class);
        } catch (RestClientException error) {
            throw new IllegalStateException("No se pudo consultar la dirección. Intenta de nuevo.");
        }
        if (respuesta != null && "OK".equals(respuesta.get("status"))
                && respuesta.get("results") instanceof List<?> resultados) {
            for (Object valor : resultados) {
                if (valor instanceof Map<?, ?> resultado && resultado.get("types") instanceof List<?> tipos
                        && (tipos.contains("street_address") || tipos.contains("premise")
                        || (inversa && tipos.contains("route")))
                        && resultado.get("formatted_address") instanceof String calle
                        && resultado.get("geometry") instanceof Map<?, ?> geometria
                        && geometria.get("location") instanceof Map<?, ?> punto
                        && punto.get("lat") instanceof Number lat && punto.get("lng") instanceof Number lng) {
                    String distritoEncontrado = distritoDe(resultado);
                    if (distritoEncontrado != null || inversa) {
                        return new Ubicacion(calle.split(",", 2)[0], distritoEncontrado,
                                inversa ? latitud : lat.doubleValue(), inversa ? longitud : lng.doubleValue());
                    }
                }
            }
        }
        throw new IllegalStateException("No encontramos la dirección. Revisa la calle, el número y el distrito.");
    }

    private static boolean valida(Double valor, int limite) {
        return valor != null && Double.isFinite(valor) && Math.abs(valor) <= limite;
    }

    private static String distritoDe(Map<?, ?> resultado) {
        if (resultado.get("address_components") instanceof List<?> componentes) {
            for (Object valor : componentes) {
                if (valor instanceof Map<?, ?> componente && componente.get("long_name") instanceof String nombre) {
                    if (nombre.equalsIgnoreCase("Ate") || nombre.equalsIgnoreCase("Ate Vitarte")) return "Ate";
                    if (nombre.matches("(?i)Lurigancho(?:[- ]Chosica)?|Chosica")) return "Lurigancho-Chosica";
                }
            }
        }
        return null;
    }
}
