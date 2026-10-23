package com.rinconcito.service.reporte;

public class FormatoReporteNoSoportadoException extends RuntimeException {
    public FormatoReporteNoSoportadoException() {
        super("Formato de reporte no soportado.");
    }
}
