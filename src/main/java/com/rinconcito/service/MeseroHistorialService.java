package com.rinconcito.service;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MeseroHistorialService {

    private static final List<EstadoPedido>
            ESTADOS_HISTORIAL = List.of(
                    EstadoPedido.FINALIZADO,
                    EstadoPedido.CANCELADO
            );

    private final PedidoRepository pedidoRepository;

    private final DetallePedidoRepository
            detallePedidoRepository;

    public MeseroHistorialService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository =
                detallePedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarHistorial(
            Usuario mesero
    ) {
        validarMesero(mesero);

        List<Pedido> pedidos =
                new ArrayList<>(
                        pedidoRepository.findByMesero(
                                mesero
                        )
                );

        pedidos.removeIf(pedido ->
                pedido.getTipoPedido()
                        != TipoPedido.PRESENCIAL
                        || !ESTADOS_HISTORIAL.contains(
                                pedido.getEstado()
                        )
        );

        pedidos.sort(
                Comparator.comparing(
                        Pedido::getFechaActualizacion,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ).reversed()
        );

        pedidos.forEach(
                this::inicializarRelaciones
        );

        return pedidos;
    }

    @Transactional(readOnly = true)
    public List<DetallePedido> listarDetalles(
            Pedido pedido
    ) {
        if (pedido == null
                || pedido.getIdPedido() == null) {

            return List.of();
        }

        List<DetallePedido> detalles =
                detallePedidoRepository
                        .findByPedido(pedido);

        detalles.forEach(detalle -> {
            if (detalle.getProducto() != null) {
                detalle.getProducto().getNombre();
                detalle.getProducto().getImagen();

                if (detalle.getProducto()
                        .getCategoria() != null) {

                    detalle.getProducto()
                            .getCategoria()
                            .getNombre();
                }
            }
        });

        return detalles;
    }

    public long contarFinalizados(
            List<Pedido> pedidos
    ) {
        if (pedidos == null) {
            return 0;
        }

        return pedidos.stream()
                .filter(pedido ->
                        pedido.getEstado()
                                == EstadoPedido.FINALIZADO
                )
                .count();
    }

    public long contarCancelados(
            List<Pedido> pedidos
    ) {
        if (pedidos == null) {
            return 0;
        }

        return pedidos.stream()
                .filter(pedido ->
                        pedido.getEstado()
                                == EstadoPedido.CANCELADO
                )
                .count();
    }

    public long sumarVentasFinalizadas(
            List<Pedido> pedidos
    ) {
        if (pedidos == null) {
            return 0;
        }

        return pedidos.stream()
                .filter(pedido ->
                        pedido.getEstado()
                                == EstadoPedido.FINALIZADO
                )
                .map(Pedido::getTotal)
                .filter(total -> total != null)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private void validarMesero(
            Usuario mesero
    ) {
        if (mesero == null
                || mesero.getIdUsuario() == null) {

            throw new IllegalStateException(
                    "No se pudo identificar al mesero."
            );
        }
    }

    private void inicializarRelaciones(
            Pedido pedido
    ) {
        if (pedido.getCliente() != null) {
            pedido.getCliente().getNombres();
            pedido.getCliente().getApellidos();
        }

        if (pedido.getMesero() != null) {
            pedido.getMesero().getNombres();
            pedido.getMesero().getApellidos();
        }

        if (pedido.getMesa() != null) {
            pedido.getMesa().getNumero();

            if (pedido.getMesa().getPiso() != null) {
                pedido.getMesa()
                        .getPiso()
                        .getNombre();
            }
        }

        LocalDateTime fecha =
                pedido.getFechaActualizacion();

        if (fecha == null) {
            pedido.getFechaRegistro();
        }
    }
}