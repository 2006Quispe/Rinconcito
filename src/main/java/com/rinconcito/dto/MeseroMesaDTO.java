package com.rinconcito.dto;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Reserva;

public class MeseroMesaDTO {

    private final Mesa mesa;
    private final Reserva reserva;
    private final String estadoVisual;

    public MeseroMesaDTO(
            Mesa mesa,
            Reserva reserva,
            String estadoVisual
    ) {
        this.mesa = mesa;
        this.reserva = reserva;
        this.estadoVisual = estadoVisual;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public String getEstadoVisual() {
        return estadoVisual;
    }

    public boolean isDisponible() {
        return "DISPONIBLE".equals(
                estadoVisual
        );
    }

    public boolean isOcupada() {
        return "OCUPADA".equals(
                estadoVisual
        );
    }

    public boolean isReservada() {
        return "RESERVADA".equals(
                estadoVisual
        );
    }

    public boolean isInactiva() {
        return "INACTIVA".equals(
                estadoVisual
        );
    }
}