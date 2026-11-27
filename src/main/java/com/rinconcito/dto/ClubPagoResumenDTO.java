package com.rinconcito.dto;

public record ClubPagoResumenDTO(
        String clienteNombre,
        String nivelCodigo,
        String nivelNombre,
        int puntosAnualesAntes,
        int saldoAnterior,
        int deudaAnterior,
        int puntosBase,
        int puntosBonusNivel,
        int puntosBonusCampana,
        String campanaNombre,
        int puntosTotales,
        int puntosAplicadosDeuda,
        int puntosAcreditados,
        int saldoNuevo,
        int deudaNueva,
        String nuevoNivelCodigo,
        String nuevoNivelNombre
) {
}
