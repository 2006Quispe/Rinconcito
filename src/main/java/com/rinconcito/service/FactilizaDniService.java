package com.rinconcito.service;

import com.rinconcito.dto.DniConsultaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Service
public class FactilizaDniService {

    private final RestClient restClient;
    private final String token;

    public FactilizaDniService(
            RestClient.Builder restClientBuilder,
            @Value("${factiliza.api.url:https://api.factiliza.com}")
            String apiUrl,
            @Value("${factiliza.api.token:}")
            String token
    ) {
        this.restClient =
                restClientBuilder
                        .baseUrl(apiUrl)
                        .build();

        this.token =
                token == null
                        ? ""
                        : token.trim();
    }

    public Optional<DniConsultaDTO> consultar(
            String dni
    ) {
        if (
                dni == null
                        || !dni.matches("^[0-9]{8}$")
        ) {
            throw new IllegalArgumentException(
                    "El DNI debe contener exactamente 8 números."
            );
        }

        if (token.isBlank()) {
            throw new DniConsultaNoDisponibleException(
                    "La consulta de DNI aún no está configurada."
            );
        }

        try {
            FactilizaDniResponse respuesta =
                    restClient
                            .get()
                            .uri(
                                    "/v1/dni/info/{dni}",
                                    dni
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + token
                            )
                            .retrieve()
                            .body(
                                    FactilizaDniResponse.class
                            );

            if (
                    respuesta == null
                            || !Boolean.TRUE.equals(
                                    respuesta.success()
                            )
                            || respuesta.data() == null
            ) {
                return Optional.empty();
            }

            FactilizaDniData datos =
                    respuesta.data();

            if (
                    esVacio(datos.nombres())
                            || esVacio(
                                    datos.apellido_paterno()
                            )
            ) {
                return Optional.empty();
            }

            String apellidos =
                    unirApellidos(
                            datos.apellido_paterno(),
                            datos.apellido_materno()
                    );

            String numero =
                    esVacio(datos.numero())
                            ? dni
                            : datos.numero().trim();

            return Optional.of(
                    new DniConsultaDTO(
                            numero,
                            datos.nombres().trim(),
                            apellidos
                    )
            );

        } catch (RestClientResponseException e) {
            int estado =
                    e.getStatusCode().value();

            if (estado == 400 || estado == 404) {
                return Optional.empty();
            }

            throw new DniConsultaNoDisponibleException(
                    "No se pudo consultar el DNI en este momento.",
                    e
            );

        } catch (ResourceAccessException e) {
            throw new DniConsultaNoDisponibleException(
                    "No se pudo conectar con el servicio de DNI.",
                    e
            );

        } catch (RestClientException e) {
            throw new DniConsultaNoDisponibleException(
                    "La respuesta del servicio de DNI no pudo procesarse.",
                    e
            );
        }
    }

    private String unirApellidos(
            String apellidoPaterno,
            String apellidoMaterno
    ) {
        if (esVacio(apellidoMaterno)) {
            return apellidoPaterno.trim();
        }

        return apellidoPaterno.trim()
                + " "
                + apellidoMaterno.trim();
    }

    private boolean esVacio(
            String valor
    ) {
        return valor == null
                || valor.isBlank();
    }

    private record FactilizaDniResponse(
            Integer status,
            Boolean success,
            String message,
            FactilizaDniData data
    ) {
    }

    private record FactilizaDniData(
            String numero,
            String nombres,
            String apellido_paterno,
            String apellido_materno
    ) {
    }
}
