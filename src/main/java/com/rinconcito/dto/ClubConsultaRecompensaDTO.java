package com.rinconcito.dto;

public record ClubConsultaRecompensaDTO(
        boolean disponible,
        String mensaje,
        String codigo,
        String nombre,
        String condiciones
) {
    public static ClubConsultaRecompensaDTO noDisponible(String codigo, String mensaje) {
        return new ClubConsultaRecompensaDTO(false, mensaje, codigo, null, null);
    }
}
