package com.rinconcito.dto;

public record ClubCobroEstimacionDTO(
        boolean clienteRegistrado,
        String mensaje,
        String clienteNombre,
        String nivelCodigo,
        String nivelNombre,
        int porcentajeBonusNivel,
        int puntosAnuales,
        int saldoDisponible,
        int puntosDeuda,
        int puntosBase,
        int puntosBonusNivel,
        int puntosBonusCampana,
        String campanaNombre,
        int totalPuntosEstimados,
        int puntosAplicadosDeuda,
        int puntosAcreditadosEstimados,
        int saldoNuevoEstimado,
        int deudaNuevaEstimada
) {
    public static ClubCobroEstimacionDTO noRegistrado() {
        return new ClubCobroEstimacionDTO(
                false, "No encontramos una cuenta activa del Club con ese DNI.",
                null, null, null, 0, 0, 0, 0, 0, 0, 0,
                null, 0, 0, 0, 0, 0
        );
    }
}
