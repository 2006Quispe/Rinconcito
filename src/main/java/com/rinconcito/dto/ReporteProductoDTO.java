package com.rinconcito.dto;

public class ReporteProductoDTO {

    private final String producto;
    private final long cantidad;
    private final long monto;

    public ReporteProductoDTO(
            String producto,
            long cantidad,
            long monto
    ) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.monto = monto;
    }

    public String getProducto() {
        return producto;
    }

    public long getCantidad() {
        return cantidad;
    }

    public long getMonto() {
        return monto;
    }
}