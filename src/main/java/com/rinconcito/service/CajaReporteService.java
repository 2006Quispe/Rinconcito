package com.rinconcito.service;

import com.rinconcito.dto.CajaReporteDTO;
import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.NotaVenta.EstadoNotaVenta;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.repository.NotaVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CajaReporteService {

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final Locale LOCALE_PERU =
            new Locale("es", "PE");

    private final NotaVentaRepository notaVentaRepository;

    public CajaReporteService(
            NotaVentaRepository notaVentaRepository
    ) {
        this.notaVentaRepository = notaVentaRepository;
    }

    @Transactional(readOnly = true)
    public CajaReporteDTO obtenerReporteDashboard() {

        LocalDate hoy = LocalDate.now(ZONA_PERU);

        LocalDateTime inicioHoy =
                hoy.atStartOfDay();

        LocalDateTime finHoy =
                hoy.plusDays(1).atStartOfDay();

        long ventasHoy = valorSeguro(
                notaVentaRepository.sumarVentasPorPeriodo(
                        EstadoNotaVenta.EMITIDA,
                        inicioHoy,
                        finHoy
                )
        );

        long notasEmitidasHoy =
                notaVentaRepository
                        .countByEstadoAndFechaEmisionBetween(
                                EstadoNotaVenta.EMITIDA,
                                inicioHoy,
                                finHoy
                        );

        List<String> etiquetasVentas =
                new ArrayList<>();

        List<Long> montosVentas =
                new ArrayList<>();

        DateTimeFormatter formatoDia =
                DateTimeFormatter.ofPattern(
                        "EEE dd",
                        LOCALE_PERU
                );

        /*
         * Ventas de los últimos siete días,
         * incluyendo el día actual.
         */
        for (int diasAtras = 6;
             diasAtras >= 0;
             diasAtras--) {

            LocalDate fecha =
                    hoy.minusDays(diasAtras);

            LocalDateTime inicio =
                    fecha.atStartOfDay();

            LocalDateTime fin =
                    fecha.plusDays(1).atStartOfDay();

            Long montoConsultado =
                    notaVentaRepository
                            .sumarVentasPorPeriodo(
                                    EstadoNotaVenta.EMITIDA,
                                    inicio,
                                    fin
                            );

            String etiqueta =
                    fecha.format(formatoDia);

            if (!etiqueta.isBlank()) {
                etiqueta =
                        etiqueta.substring(0, 1)
                                .toUpperCase()
                                + etiqueta.substring(1);
            }

            etiquetasVentas.add(etiqueta);
            montosVentas.add(
                    valorSeguro(montoConsultado)
            );
        }

        /*
         * Distribución de métodos de pago
         * durante los últimos siete días.
         */
        LocalDateTime inicioPeriodo =
                hoy.minusDays(6).atStartOfDay();

        LocalDateTime finPeriodo =
                hoy.plusDays(1).atStartOfDay();

        List<Object[]> resumenMetodos =
                notaVentaRepository
                        .resumirPorMetodoPago(
                                EstadoNotaVenta.EMITIDA,
                                inicioPeriodo,
                                finPeriodo
                        );

        List<String> etiquetasMetodos =
                new ArrayList<>();

        List<Long> cantidadesMetodos =
                new ArrayList<>();

        List<Long> montosMetodos =
                new ArrayList<>();

        if (resumenMetodos != null) {

            for (Object[] fila : resumenMetodos) {

                if (fila == null || fila.length < 3) {
                    continue;
                }

                MetodoPago metodo =
                        convertirMetodoPago(fila[0]);

                long cantidad =
                        convertirNumero(fila[1]);

                long monto =
                        convertirNumero(fila[2]);

                etiquetasMetodos.add(
                        nombreMetodo(metodo)
                );

                cantidadesMetodos.add(cantidad);
                montosMetodos.add(monto);
            }
        }

        List<NotaVenta> ultimasNotas =
                notaVentaRepository
                        .findTop5ByOrderByFechaEmisionDesc();

        if (ultimasNotas == null) {
            ultimasNotas = new ArrayList<>();
        }

        return new CajaReporteDTO(
                ventasHoy,
                notasEmitidasHoy,
                etiquetasVentas,
                montosVentas,
                etiquetasMetodos,
                cantidadesMetodos,
                montosMetodos,
                ultimasNotas
        );
    }

    private long valorSeguro(Long valor) {
        return valor != null ? valor : 0L;
    }

    private long convertirNumero(Object valor) {

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        if (valor == null) {
            return 0L;
        }

        try {
            return Long.parseLong(valor.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private MetodoPago convertirMetodoPago(
            Object valor
    ) {
        if (valor instanceof MetodoPago metodoPago) {
            return metodoPago;
        }

        if (valor == null) {
            return null;
        }

        try {
            return MetodoPago.valueOf(
                    valor.toString().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String nombreMetodo(
            MetodoPago metodoPago
    ) {
        if (metodoPago == null) {
            return "Otro";
        }

        return switch (metodoPago) {
            case YAPE -> "Yape";
            case PLIN -> "Plin";
            case TRANSFERENCIA -> "Transferencia";
            case EFECTIVO -> "Efectivo";
        };
    }
}