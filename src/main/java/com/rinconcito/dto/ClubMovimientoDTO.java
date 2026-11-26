package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubMovimientoDTO(
        String tipo,
        Integer puntos,
        String descripcion,
        String referencia,
        LocalDateTime creadoEn,
        LocalDateTime venceEn
) {
}
