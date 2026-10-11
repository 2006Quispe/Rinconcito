package com.rinconcito.dto;

public class ItemPedidoPresencialDTO {

    private Integer idProducto;
    private Integer cantidad;
    private String observaciones;

    public ItemPedidoPresencialDTO() {
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(
            Integer idProducto
    ) {
        this.idProducto = idProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(
            Integer cantidad
    ) {
        this.cantidad = cantidad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones = observaciones;
    }
}