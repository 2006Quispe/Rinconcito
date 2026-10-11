package com.rinconcito.dto;

import java.time.LocalDate;

public class ReporteDiaDTO {

    private final LocalDate fecha;
    private final String etiqueta;
    private final long monto;

    public ReporteDiaDTO(
            LocalDate fecha,
            String etiqueta,
            long monto
    ) {
        this.fecha = fecha;
        this.etiqueta = etiqueta;
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public long getMonto() {
        return monto;
    }
}