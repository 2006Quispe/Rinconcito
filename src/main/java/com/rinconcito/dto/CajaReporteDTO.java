package com.rinconcito.dto;

import com.rinconcito.entity.NotaVenta;

import java.util.ArrayList;
import java.util.List;

public class CajaReporteDTO {

    private long ventasHoy;
    private long notasEmitidasHoy;

    private List<String> etiquetasVentas;
    private List<Long> montosVentas;

    private List<String> etiquetasMetodos;
    private List<Long> cantidadesMetodos;
    private List<Long> montosMetodos;

    private List<NotaVenta> ultimasNotas;

    public CajaReporteDTO() {
        this.etiquetasVentas = new ArrayList<>();
        this.montosVentas = new ArrayList<>();
        this.etiquetasMetodos = new ArrayList<>();
        this.cantidadesMetodos = new ArrayList<>();
        this.montosMetodos = new ArrayList<>();
        this.ultimasNotas = new ArrayList<>();
    }

    public CajaReporteDTO(
            long ventasHoy,
            long notasEmitidasHoy,
            List<String> etiquetasVentas,
            List<Long> montosVentas,
            List<String> etiquetasMetodos,
            List<Long> cantidadesMetodos,
            List<Long> montosMetodos,
            List<NotaVenta> ultimasNotas
    ) {
        this.ventasHoy = ventasHoy;
        this.notasEmitidasHoy = notasEmitidasHoy;
        this.etiquetasVentas =
                etiquetasVentas != null
                        ? etiquetasVentas
                        : new ArrayList<>();

        this.montosVentas =
                montosVentas != null
                        ? montosVentas
                        : new ArrayList<>();

        this.etiquetasMetodos =
                etiquetasMetodos != null
                        ? etiquetasMetodos
                        : new ArrayList<>();

        this.cantidadesMetodos =
                cantidadesMetodos != null
                        ? cantidadesMetodos
                        : new ArrayList<>();

        this.montosMetodos =
                montosMetodos != null
                        ? montosMetodos
                        : new ArrayList<>();

        this.ultimasNotas =
                ultimasNotas != null
                        ? ultimasNotas
                        : new ArrayList<>();
    }

    public boolean tieneVentas() {
        return montosVentas != null
                && montosVentas.stream()
                .anyMatch(monto -> monto != null && monto > 0);
    }

    public boolean tieneMetodosPago() {
        return cantidadesMetodos != null
                && cantidadesMetodos.stream()
                .anyMatch(cantidad ->
                        cantidad != null && cantidad > 0
                );
    }

    public long getVentasHoy() {
        return ventasHoy;
    }

    public void setVentasHoy(long ventasHoy) {
        this.ventasHoy = ventasHoy;
    }

    public long getNotasEmitidasHoy() {
        return notasEmitidasHoy;
    }

    public void setNotasEmitidasHoy(long notasEmitidasHoy) {
        this.notasEmitidasHoy = notasEmitidasHoy;
    }

    public List<String> getEtiquetasVentas() {
        return etiquetasVentas;
    }

    public void setEtiquetasVentas(
            List<String> etiquetasVentas
    ) {
        this.etiquetasVentas = etiquetasVentas;
    }

    public List<Long> getMontosVentas() {
        return montosVentas;
    }

    public void setMontosVentas(
            List<Long> montosVentas
    ) {
        this.montosVentas = montosVentas;
    }

    public List<String> getEtiquetasMetodos() {
        return etiquetasMetodos;
    }

    public void setEtiquetasMetodos(
            List<String> etiquetasMetodos
    ) {
        this.etiquetasMetodos = etiquetasMetodos;
    }

    public List<Long> getCantidadesMetodos() {
        return cantidadesMetodos;
    }

    public void setCantidadesMetodos(
            List<Long> cantidadesMetodos
    ) {
        this.cantidadesMetodos = cantidadesMetodos;
    }

    public List<Long> getMontosMetodos() {
        return montosMetodos;
    }

    public void setMontosMetodos(
            List<Long> montosMetodos
    ) {
        this.montosMetodos = montosMetodos;
    }

    public List<NotaVenta> getUltimasNotas() {
        return ultimasNotas;
    }

    public void setUltimasNotas(
            List<NotaVenta> ultimasNotas
    ) {
        this.ultimasNotas = ultimasNotas;
    }
}