package com.rinconcito.dto;

import java.time.LocalDateTime;

public record ClubRecompensaDTO(
        Integer idRecompensa,
        String codigo,
        String nombre,
        String descripcion,
        Integer costoPuntos,
        String tipo,
        String categoria,
        Integer valor,
        Integer compraMinima,
        String condiciones,
        String imagen,
        Integer stock,
        Integer vigenciaDias,
        Integer idProducto,
        Integer precioReferencia,
        LocalDateTime disponibleDesde,
        LocalDateTime disponibleHasta,
        boolean canjeable,
        String estadoDisponibilidad,
        int puntosFaltantes,
        int progresoPorcentaje
) {
    public String getModalidades() {
        return switch (tipo) {
            case "DELIVERY_GRATIS" -> "Delivery";
            case "EXPERIENCIA" -> "Presencial";
            default -> "Presencial · Delivery · Recojo en local";
        };
    }
}
