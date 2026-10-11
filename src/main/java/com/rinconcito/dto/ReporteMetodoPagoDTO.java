package com.rinconcito.dto;

public class ReporteMetodoPagoDTO {

    private final String metodo;
    private final long cantidad;
    private final long monto;

    public ReporteMetodoPagoDTO(
            String metodo,
            long cantidad,
            long monto
    ) {
        this.metodo = metodo;
        this.cantidad = cantidad;
        this.monto = monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public long getCantidad() {
        return cantidad;
    }

    public long getMonto() {
        return monto;
    }
}