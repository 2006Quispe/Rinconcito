package com.rinconcito.dto;

import java.util.ArrayList;
import java.util.List;

public class PedidoPresencialFormDTO {

    private Integer idMesa;
    private Integer idReserva;
    private String observaciones;

    private List<ItemPedidoPresencialDTO> items =
            new ArrayList<>();

    public PedidoPresencialFormDTO() {
    }

    public Integer getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Integer idMesa) {
        this.idMesa = idMesa;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<ItemPedidoPresencialDTO> getItems() {
        return items;
    }

    public void setItems(
            List<ItemPedidoPresencialDTO> items
    ) {
        this.items = items != null
                ? items
                : new ArrayList<>();
    }
}