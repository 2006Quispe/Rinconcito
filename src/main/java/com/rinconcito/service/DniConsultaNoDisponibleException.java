package com.rinconcito.service;

public class DniConsultaNoDisponibleException
        extends RuntimeException {

    public DniConsultaNoDisponibleException(
            String message
    ) {
        super(message);
    }

    public DniConsultaNoDisponibleException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
