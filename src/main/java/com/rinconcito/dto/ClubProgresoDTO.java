package com.rinconcito.dto;

public record ClubProgresoDTO(
        ClubRecompensaDTO recompensa,
        int saldo,
        int puntosNecesarios,
        int puntosFaltantes,
        int porcentaje,
        boolean puedeCanjear,
        String mensaje
) {
}
