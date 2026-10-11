package com.rinconcito.dto;

import com.rinconcito.entity.Pago;

import java.time.LocalDate;
import java.util.List;

public class AdminReporteDTO {

    private final LocalDate desde;
    private final LocalDate hasta;

    private final long totalVentas;
    private final long totalOperaciones;
    private final long aprobados;
    private final long rechazados;
    private final long pendientes;
    private final long pedidosAprobados;
    private final long reservasAprobadas;

    private final List<ReporteDiaDTO> ventasDiarias;
    private final List<ReporteMetodoPagoDTO> metodosPago;
    private final List<ReporteProductoDTO> productosVendidos;
    private final List<Pago> operaciones;

    public AdminReporteDTO(
            LocalDate desde,
            LocalDate hasta,
            long totalVentas,
            long totalOperaciones,
            long aprobados,
            long rechazados,
            long pendientes,
            long pedidosAprobados,
            long reservasAprobadas,
            List<ReporteDiaDTO> ventasDiarias,
            List<ReporteMetodoPagoDTO> metodosPago,
            List<ReporteProductoDTO> productosVendidos,
            List<Pago> operaciones
    ) {
        this.desde = desde;
        this.hasta = hasta;
        this.totalVentas = totalVentas;
        this.totalOperaciones = totalOperaciones;
        this.aprobados = aprobados;
        this.rechazados = rechazados;
        this.pendientes = pendientes;
        this.pedidosAprobados = pedidosAprobados;
        this.reservasAprobadas = reservasAprobadas;
        this.ventasDiarias = ventasDiarias;
        this.metodosPago = metodosPago;
        this.productosVendidos = productosVendidos;
        this.operaciones = operaciones;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public long getTotalVentas() {
        return totalVentas;
    }

    public long getTotalOperaciones() {
        return totalOperaciones;
    }

    public long getAprobados() {
        return aprobados;
    }

    public long getRechazados() {
        return rechazados;
    }

    public long getPendientes() {
        return pendientes;
    }

    public long getPedidosAprobados() {
        return pedidosAprobados;
    }

    public long getReservasAprobadas() {
        return reservasAprobadas;
    }

    public List<ReporteDiaDTO> getVentasDiarias() {
        return ventasDiarias;
    }

    public List<ReporteMetodoPagoDTO> getMetodosPago() {
        return metodosPago;
    }

    public List<ReporteProductoDTO> getProductosVendidos() {
        return productosVendidos;
    }

    public List<Pago> getOperaciones() {
        return operaciones;
    }
}