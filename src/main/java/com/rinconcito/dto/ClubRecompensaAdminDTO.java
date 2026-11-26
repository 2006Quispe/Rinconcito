package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubRecompensaAdminDTO(
        Integer idRecompensa,
        String codigo,
        String nombre,
        String tipo,
        String categoria,
        Integer costoPuntos,
        Integer stock,
        boolean activo,
        LocalDateTime disponibleDesde,
        LocalDateTime disponibleHasta,
        Long totalCanjes
) {
}
