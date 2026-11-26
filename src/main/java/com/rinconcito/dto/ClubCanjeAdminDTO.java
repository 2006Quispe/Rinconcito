package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubCanjeAdminDTO(
        Long idCanje,
        String cliente,
        String dni,
        String recompensa,
        String codigo,
        String categoria,
        String estado,
        Integer puntosUsados,
        LocalDateTime fechaCanje,
        LocalDateTime venceEn,
        String validador
) {
}
