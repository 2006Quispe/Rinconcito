package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubCuponDTO(
        Long idCanje,
        String nombreRecompensa,
        String codigoCupon,
        String estado,
        Integer puntosUsados,
        LocalDateTime fechaCanje,
        LocalDateTime venceEn,
        String tipo,
        String categoria,
        Integer valor,
        Integer compraMinima,
        String condiciones
) {
}
