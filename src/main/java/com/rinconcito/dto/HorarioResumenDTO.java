package com.rinconcito.dto;

public class HorarioResumenDTO {

    private String dias;
    private String detalle;

    public HorarioResumenDTO() {
    }

    public HorarioResumenDTO(String dias, String detalle) {
        this.dias = dias;
        this.detalle = detalle;
    }

    public String getDias() {
        return dias;
    }

    public void setDias(String dias) {
        this.dias = dias;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
