package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubCampanaAdminDTO(
        Long idCampana,
        String nombre,
        Integer porcentajeBonus,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean activa
) {
}
