package com.rinconcito.dto;

public record ClubNivelDTO(
        String codigo,
        String nombre,
        int puntosAnuales,
        int porcentajeBonus,
        String beneficio,
        String siguienteNivel,
        Integer puntosMeta,
        int progresoPorcentaje
) {
}
