package com.rinconcito.service;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminPedidoService {

    private final PedidoRepository pedidoRepository;

    private final DetallePedidoRepository
            detallePedidoRepository;

    public AdminPedidoService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository
                    detallePedidoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository =
                detallePedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarConFiltros(
            LocalDate fecha,
            EstadoPedido estado,
            TipoPedido tipoPedido,
            String busqueda
    ) {
        LocalDateTime inicio = null;
        LocalDateTime fin = null;

        if (fecha != null) {
            inicio = fecha.atStartOfDay();
            fin = fecha.plusDays(1).atStartOfDay();
        }

        String textoBusqueda =
                normalizarBusqueda(busqueda);

        return pedidoRepository
                .buscarParaAdministracion(
                        inicio,
                        fin,
                        estado,
                        tipoPedido,
                        textoBusqueda
                );
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<DetallePedido>>
    obtenerDetallesPorPedido(
            List<Pedido> pedidos
    ) {
        Map<Integer, List<DetallePedido>>
                detallesPorPedido =
                new LinkedHashMap<>();

        if (pedidos == null || pedidos.isEmpty()) {
            return detallesPorPedido;
        }

        for (Pedido pedido : pedidos) {
            detallesPorPedido.put(
                    pedido.getIdPedido(),
                    new java.util.ArrayList<>()
            );
        }

        List<DetallePedido> detalles =
                detallePedidoRepository
                        .findByPedidoIn(pedidos);

        for (DetallePedido detalle : detalles) {
            if (detalle.getPedido() == null
                    || detalle.getPedido()
                    .getIdPedido() == null) {
                continue;
            }

            Integer idPedido =
                    detalle.getPedido()
                            .getIdPedido();

            detallesPorPedido
                    .computeIfAbsent(
                            idPedido,
                            clave ->
                                    new java.util.ArrayList<>()
                    )
                    .add(detalle);
        }

        return detallesPorPedido;
    }

    public long contarPorEstado(
            List<Pedido> pedidos,
            EstadoPedido estado
    ) {
        if (pedidos == null || estado == null) {
            return 0;
        }

        return pedidos.stream()
                .filter(pedido ->
                        pedido.getEstado() == estado
                )
                .count();
    }

    public int sumarTotales(
            List<Pedido> pedidos
    ) {
        if (pedidos == null) {
            return 0;
        }

        return pedidos.stream()
                .filter(pedido ->
                        pedido.getEstado()
                                != EstadoPedido.CANCELADO
                )
                .map(Pedido::getTotal)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String normalizarBusqueda(
            String busqueda
    ) {
        if (busqueda == null
                || busqueda.isBlank()) {
            return null;
        }

        return busqueda.trim();
    }
}