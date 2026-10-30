package com.rinconcito.service;

import com.rinconcito.dto.DniConsultaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FactilizaDniServiceTest {

    @Test
    void consultaYCombinaLosApellidos() {
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer servidor =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        FactilizaDniService servicio =
                new FactilizaDniService(
                        builder,
                        "https://api.factiliza.com",
                        "token-prueba"
                );

        servidor.expect(
                        once(),
                        requestTo(
                                "https://api.factiliza.com/v1/dni/info/27427864"
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andExpect(
                        header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer token-prueba"
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "status": 200,
                                  "success": true,
                                  "message": "Exito",
                                  "data": {
                                    "numero": "27427864",
                                    "nombres": "JOSE PEDRO",
                                    "apellido_paterno": "CASTILLO",
                                    "apellido_materno": "TERRONES"
                                  }
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        Optional<DniConsultaDTO> resultado =
                servicio.consultar(
                        "27427864"
                );

        assertTrue(resultado.isPresent());
        assertEquals(
                "JOSE PEDRO",
                resultado.get().nombres()
        );
        assertEquals(
                "CASTILLO TERRONES",
                resultado.get().apellidos()
        );

        servidor.verify();
    }

    @Test
    void devuelveVacioCuandoElDniNoExiste() {
        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer servidor =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        FactilizaDniService servicio =
                new FactilizaDniService(
                        builder,
                        "https://api.factiliza.com",
                        "token-prueba"
                );

        servidor.expect(
                        requestTo(
                                "https://api.factiliza.com/v1/dni/info/00000000"
                        )
                )
                .andRespond(
                        withStatus(
                                HttpStatus.BAD_REQUEST
                        )
                );

        assertTrue(
                servicio.consultar(
                        "00000000"
                ).isEmpty()
        );

        servidor.verify();
    }

    @Test
    void rechazaConfiguracionSinToken() {
        FactilizaDniService servicio =
                new FactilizaDniService(
                        RestClient.builder(),
                        "https://api.factiliza.com",
                        ""
                );

        assertThrows(
                DniConsultaNoDisponibleException.class,
                () -> servicio.consultar(
                        "27427864"
                )
        );
    }

    @Test
    void rechazaDniConFormatoInvalido() {
        FactilizaDniService servicio =
                new FactilizaDniService(
                        RestClient.builder(),
                        "https://api.factiliza.com",
                        "token-prueba"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(
                        "1234"
                )
        );
    }
}
