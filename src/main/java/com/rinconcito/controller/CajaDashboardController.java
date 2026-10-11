package com.rinconcito.controller;

import com.rinconcito.dto.CajaReporteDTO;
import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.service.CajaReporteService;
import com.rinconcito.service.NotaVentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/caja")
public class CajaDashboardController {

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final Locale IDIOMA_PERU =
            Locale.forLanguageTag("es-PE");

    private static final DateTimeFormatter
            FORMATO_FECHA =
            DateTimeFormatter.ofPattern(
                    "EEEE d 'de' MMMM 'de' yyyy",
                    IDIOMA_PERU
            );

    private final PagoRepository pagoRepository;
    private final CajaReporteService cajaReporteService;
    private final NotaVentaService notaVentaService;

    public CajaDashboardController(
            PagoRepository pagoRepository,
            CajaReporteService cajaReporteService,
            NotaVentaService notaVentaService
    ) {
        this.pagoRepository =
                pagoRepository;

        this.cajaReporteService =
                cajaReporteService;

        this.notaVentaService =
                notaVentaService;
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(
            Model model
    ) {
        LocalDate hoy =
                LocalDate.now(ZONA_PERU);

        LocalDateTime inicioDia =
                hoy.atStartOfDay();

        /*
         * El final se mantiene como el inicio del día siguiente,
         * porque PagoRepository utiliza un periodo semiabierto.
         */
        LocalDateTime finDia =
                hoy.plusDays(1).atStartOfDay();

        long pedidosPendientes =
                pagoRepository
                        .countByEstadoAndPedidoIsNotNull(
                                EstadoPago.PENDIENTE_VALIDACION
                        );

        long reservasPendientes =
                pagoRepository
                        .countByEstadoAndReservaIsNotNull(
                                EstadoPago.PENDIENTE_VALIDACION
                        );

        long aprobadosHoy =
                pagoRepository
                        .contarValidadosEnFecha(
                                EstadoPago.APROBADO,
                                inicioDia,
                                finDia
                        );

        long rechazadosHoy =
                pagoRepository
                        .contarValidadosEnFecha(
                                EstadoPago.RECHAZADO,
                                inicioDia,
                                finDia
                        );

        long totalPendientes =
                pedidosPendientes
                        + reservasPendientes;

        CajaReporteDTO reporte =
                cajaReporteService
                        .obtenerReporteDashboard();

        /*
         * Números formateados de las últimas notas.
         */
        Map<Integer, String> numerosUltimasNotas =
                new LinkedHashMap<>();

        for (
                NotaVenta nota :
                reporte.getUltimasNotas()
        ) {
            numerosUltimasNotas.put(
                    nota.getIdNota(),
                    notaVentaService.formarNumero(nota)
            );
        }

        model.addAttribute(
                "titulo",
                "Panel de Caja"
        );

        model.addAttribute(
                "rol",
                "CAJA"
        );

        model.addAttribute(
                "fechaActual",
                formatearFecha(hoy)
        );

        /*
         * Contadores de validación.
         */
        model.addAttribute(
                "pedidosPendientes",
                pedidosPendientes
        );

        model.addAttribute(
                "reservasPendientes",
                reservasPendientes
        );

        model.addAttribute(
                "totalPendientes",
                totalPendientes
        );

        model.addAttribute(
                "aprobadosHoy",
                aprobadosHoy
        );

        model.addAttribute(
                "rechazadosHoy",
                rechazadosHoy
        );

        /*
         * Estadísticas de ventas.
         */
        model.addAttribute(
                "ventasHoy",
                reporte.getVentasHoy()
        );

        model.addAttribute(
                "notasEmitidasHoy",
                reporte.getNotasEmitidasHoy()
        );

        /*
         * Datos para el gráfico de ventas.
         */
        model.addAttribute(
                "etiquetasVentas",
                reporte.getEtiquetasVentas()
        );

        model.addAttribute(
                "montosVentas",
                reporte.getMontosVentas()
        );

        /*
         * Datos para el gráfico de métodos de pago.
         */
        model.addAttribute(
                "etiquetasMetodos",
                reporte.getEtiquetasMetodos()
        );

        model.addAttribute(
                "cantidadesMetodos",
                reporte.getCantidadesMetodos()
        );

        model.addAttribute(
                "montosMetodos",
                reporte.getMontosMetodos()
        );

        model.addAttribute(
                "tieneVentas",
                reporte.tieneVentas()
        );

        model.addAttribute(
                "tieneMetodosPago",
                reporte.tieneMetodosPago()
        );

        /*
         * Últimas notas generadas.
         */
        model.addAttribute(
                "ultimasNotas",
                reporte.getUltimasNotas()
        );

        model.addAttribute(
                "numerosUltimasNotas",
                numerosUltimasNotas
        );

        return "paginas/caja/dashboard";
    }

    private String formatearFecha(
            LocalDate fecha
    ) {
        String texto =
                fecha.format(FORMATO_FECHA);

        if (texto.isBlank()) {
            return texto;
        }

        return texto.substring(0, 1)
                .toUpperCase(IDIOMA_PERU)
                + texto.substring(1);
    }
}