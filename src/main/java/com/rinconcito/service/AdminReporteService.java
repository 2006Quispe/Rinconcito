package com.rinconcito.service;

import com.google.common.util.concurrent.AtomicLongMap;
import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteDiaDTO;
import com.rinconcito.dto.ReporteMetodoPagoDTO;
import com.rinconcito.dto.ReporteProductoDTO;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminReporteService {

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final Locale LOCALE_PERU =
            new Locale("es", "PE");

    private static final int MAXIMO_DIAS_REPORTE = 366;

    private final PagoRepository pagoRepository;

    private final DetallePedidoRepository
            detallePedidoRepository;

    public AdminReporteService(
            PagoRepository pagoRepository,
            DetallePedidoRepository detallePedidoRepository
    ) {
        this.pagoRepository =
                pagoRepository;

        this.detallePedidoRepository =
                detallePedidoRepository;
    }

    @Transactional(readOnly = true)
    public AdminReporteDTO generar(
            LocalDate desde,
            LocalDate hasta
    ) {
        LocalDate hoy =
                LocalDate.now(ZONA_PERU);

        if (desde == null) {
            desde = hoy.withDayOfMonth(1);
        }

        if (hasta == null) {
            hasta = hoy;
        }

        validarPeriodo(desde, hasta);

        LocalDateTime inicio =
                desde.atStartOfDay();

        LocalDateTime fin =
                hasta.plusDays(1).atStartOfDay();

        List<Pago> operaciones =
                pagoRepository
                        .listarOperacionesParaReporte(
                                inicio,
                                fin
                        );

        if (operaciones == null) {
            operaciones = new ArrayList<>();
        }

        long totalVentas = 0;
        long aprobados = 0;
        long rechazados = 0;
        long pendientes = 0;
        long pedidosAprobados = 0;
        long reservasAprobadas = 0;

        Map<LocalDate, Long> ventasPorDia =
                inicializarDias(desde, hasta);

        Map<MetodoPago, MetodoAcumulado>
                acumuladoMetodos =
                new EnumMap<>(MetodoPago.class);

        List<Pedido> pedidosPagados =
                new ArrayList<>();

        for (Pago pago : operaciones) {

            if (
                    pago.getEstado()
                            == EstadoPago.APROBADO
            ) {
                aprobados++;

                long monto =
                        valorSeguro(pago.getMonto());

                totalVentas += monto;

                LocalDate fechaOperacion =
                        obtenerFechaOperacion(pago);

                if (
                        fechaOperacion != null
                                && ventasPorDia.containsKey(
                                        fechaOperacion
                                )
                ) {
                    ventasPorDia.compute(
                            fechaOperacion,
                            (fecha, totalActual) ->
                                    valorSeguro(totalActual)
                                            + monto
                    );
                }

                if (pago.getMetodoPago() != null) {
                    MetodoAcumulado acumulado =
                            acumuladoMetodos
                                    .computeIfAbsent(
                                            pago.getMetodoPago(),
                                            metodo ->
                                                    new MetodoAcumulado()
                                    );

                    acumulado.cantidad++;
                    acumulado.monto += monto;
                }

                if (pago.getPedido() != null) {
                    pedidosAprobados++;

                    boolean pedidoYaAgregado =
                            pedidosPagados
                                    .stream()
                                    .anyMatch(pedido ->
                                            pedido.getIdPedido()
                                                    .equals(
                                                            pago
                                                                    .getPedido()
                                                                    .getIdPedido()
                                                    )
                                    );

                    if (!pedidoYaAgregado) {
                        pedidosPagados.add(
                                pago.getPedido()
                        );
                    }
                }

                if (pago.getReserva() != null) {
                    reservasAprobadas++;
                }

            } else if (
                    pago.getEstado()
                            == EstadoPago.RECHAZADO
            ) {
                rechazados++;

            } else if (
                    pago.getEstado()
                            == EstadoPago.PENDIENTE_VALIDACION
            ) {
                pendientes++;
            }
        }

        List<ReporteDiaDTO> ventasDiarias =
                construirVentasDiarias(
                        ventasPorDia
                );

        List<ReporteMetodoPagoDTO> metodosPago =
                construirMetodosPago(
                        acumuladoMetodos
                );

        List<ReporteProductoDTO> productosVendidos =
                construirProductosVendidos(
                        pedidosPagados
                );

        return new AdminReporteDTO(
                desde,
                hasta,
                totalVentas,
                operaciones.size(),
                aprobados,
                rechazados,
                pendientes,
                pedidosAprobados,
                reservasAprobadas,
                ventasDiarias,
                metodosPago,
                productosVendidos,
                operaciones
        );
    }

    private void validarPeriodo(
            LocalDate desde,
            LocalDate hasta
    ) {
        if (desde.isAfter(hasta)) {
            throw new IllegalStateException(
                    "La fecha inicial no puede ser posterior "
                            + "a la fecha final."
            );
        }

        long cantidadDias =
                java.time.temporal.ChronoUnit.DAYS
                        .between(desde, hasta)
                        + 1;

        if (cantidadDias > MAXIMO_DIAS_REPORTE) {
            throw new IllegalStateException(
                    "El reporte no puede superar "
                            + MAXIMO_DIAS_REPORTE
                            + " días."
            );
        }
    }

    private Map<LocalDate, Long> inicializarDias(
            LocalDate desde,
            LocalDate hasta
    ) {
        Map<LocalDate, Long> dias =
                new LinkedHashMap<>();

        LocalDate fecha = desde;

        while (!fecha.isAfter(hasta)) {
            dias.put(fecha, 0L);
            fecha = fecha.plusDays(1);
        }

        return dias;
    }

    private List<ReporteDiaDTO> construirVentasDiarias(
            Map<LocalDate, Long> ventasPorDia
    ) {
        List<ReporteDiaDTO> resultado =
                new ArrayList<>();

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "dd MMM",
                        LOCALE_PERU
                );

        ventasPorDia.forEach(
                (fecha, monto) -> {
                    String etiqueta =
                            fecha.format(formato);

                    if (!etiqueta.isBlank()) {
                        etiqueta =
                                etiqueta.substring(0, 1)
                                        .toUpperCase()
                                        + etiqueta.substring(1);
                    }

                    resultado.add(
                            new ReporteDiaDTO(
                                    fecha,
                                    etiqueta,
                                    monto
                            )
                    );
                }
        );

        return resultado;
    }

    private List<ReporteMetodoPagoDTO>
    construirMetodosPago(
            Map<MetodoPago, MetodoAcumulado> acumulados
    ) {
        List<ReporteMetodoPagoDTO> resultado =
                new ArrayList<>();

        acumulados.forEach(
                (metodo, acumulado) ->
                        resultado.add(
                                new ReporteMetodoPagoDTO(
                                        nombreMetodo(metodo),
                                        acumulado.cantidad,
                                        acumulado.monto
                                )
                        )
        );

        resultado.sort(
                (primero, segundo) ->
                        Long.compare(
                                segundo.getMonto(),
                                primero.getMonto()
                        )
        );

        return resultado;
    }

    private List<ReporteProductoDTO>
    construirProductosVendidos(
            List<Pedido> pedidosPagados
    ) {
        if (pedidosPagados.isEmpty()) {
            return new ArrayList<>();
        }

        List<DetallePedido> detalles =
                detallePedidoRepository
                        .findByPedidoIn(pedidosPagados);

        Map<Integer, ProductoAcumulado>
                acumulados =
                new LinkedHashMap<>();

        // Contadores long por producto: conservar el rango del reporte.
        AtomicLongMap<Integer> unidadesVendidas = AtomicLongMap.create();

        for (DetallePedido detalle : detalles) {

            Producto producto =
                    detalle.getProducto();

            if (
                    producto == null
                            || producto.getIdProducto() == null
            ) {
                continue;
            }

            ProductoAcumulado acumulado =
                    acumulados.computeIfAbsent(
                            producto.getIdProducto(),
                            id -> new ProductoAcumulado(
                                    producto.getNombre()
                            )
                    );

            unidadesVendidas.addAndGet(
                    producto.getIdProducto(),
                    valorSeguro(
                            detalle.getCantidad()
                    )
            );

            acumulado.monto +=
                    valorSeguro(
                            detalle.getSubtotal()
                    );
        }

        List<ReporteProductoDTO> resultado =
                acumulados.entrySet()
                        .stream()
                        .map(entrada ->
                                new ReporteProductoDTO(
                                        entrada.getValue().nombre,
                                        unidadesVendidas.get(entrada.getKey()),
                                        entrada.getValue().monto
                                )
                        )
                        .sorted(
                                (primero, segundo) -> {
                                    int compararCantidad =
                                            Long.compare(
                                                    segundo.getCantidad(),
                                                    primero.getCantidad()
                                            );

                                    if (compararCantidad != 0) {
                                        return compararCantidad;
                                    }

                                    return Long.compare(
                                            segundo.getMonto(),
                                            primero.getMonto()
                                    );
                                }
                        )
                        .limit(10)
                        .toList();

        return new ArrayList<>(resultado);
    }

    private LocalDate obtenerFechaOperacion(
            Pago pago
    ) {
        LocalDateTime fecha =
                pago.getFechaValidacion() != null
                        ? pago.getFechaValidacion()
                        : pago.getFechaPago();

        return fecha != null
                ? fecha.toLocalDate()
                : null;
    }

    private long valorSeguro(
            Number valor
    ) {
        return valor != null
                ? valor.longValue()
                : 0L;
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

    private static class MetodoAcumulado {

        private long cantidad;
        private long monto;
    }

    private static class ProductoAcumulado {

        private final String nombre;
        private long monto;

        private ProductoAcumulado(
                String nombre
        ) {
            this.nombre =
                    nombre != null
                            && !nombre.isBlank()
                            ? nombre
                            : "Producto";
        }
    }
}
