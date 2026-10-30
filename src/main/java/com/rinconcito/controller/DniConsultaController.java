package com.rinconcito.controller;

import com.rinconcito.dto.DniConsultaDTO;
import com.rinconcito.service.DniConsultaNoDisponibleException;
import com.rinconcito.service.FactilizaDniService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/registro")
public class DniConsultaController {

    private final FactilizaDniService
            factilizaDniService;

    public DniConsultaController(
            FactilizaDniService factilizaDniService
    ) {
        this.factilizaDniService =
                factilizaDniService;
    }

    @PostMapping("/consultar-dni")
    public ResponseEntity<?> consultar(
            @RequestParam String dni
    ) {
        if (
                dni == null
                        || !dni.matches("^[0-9]{8}$")
        ) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "mensaje",
                                    "Ingresa un DNI válido de 8 números."
                            )
                    );
        }

        try {
            Optional<DniConsultaDTO> resultado =
                    factilizaDniService.consultar(
                            dni
                    );

            if (resultado.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                Map.of(
                                        "mensaje",
                                        "No se encontraron datos para el DNI ingresado."
                                )
                        );
            }

            return ResponseEntity.ok(
                    resultado.get()
            );

        } catch (DniConsultaNoDisponibleException e) {
            return ResponseEntity
                    .status(
                            HttpStatus.SERVICE_UNAVAILABLE
                    )
                    .body(
                            Map.of(
                                    "mensaje",
                                    e.getMessage()
                            )
                    );
        }
    }
}
