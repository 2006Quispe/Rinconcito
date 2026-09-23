package com.rinconcito.service;

import com.rinconcito.dto.ItemPedidoDTO;
import com.rinconcito.dto.PedidoFormDTO;
import com.rinconcito.entity.*;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PedidoService {

    private static final int COSTO_DELIVERY = 8;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    public Pedido crearPedido(PedidoFormDTO dto) {

        boolean tieneItems = dto.getItems().stream().anyMatch(i -> i.getCantidad() != null && i.getCantidad() > 0);
        if (!tieneItems) {
            throw new IllegalStateException("Debes elegir al menos un plato.");
        }

        TipoPedido tipo = TipoPedido.valueOf(dto.getTipoPedido());

        if (tipo == TipoPedido.DELIVERY && (dto.getDireccionTexto() == null || dto.getDireccionTexto().isBlank())) {
            throw new IllegalStateException("Debes ingresar una dirección para el delivery.");
        }

        Usuario cliente = usuarioService.buscarOCrearInvitado(
                dto.getDni(), dto.getNombres(), dto.getApellidos(), dto.getTelefono());

        Direccion direccion = null;
        if (tipo == TipoPedido.DELIVERY) {
            direccion = new Direccion();
            direccion.setUsuario(cliente);
            direccion.setDireccion(dto.getDireccionTexto());
            direccion.setPrincipal(false);
            direccion = direccionRepository.save(direccion);
        }

        int subtotal = 0;
        for (ItemPedidoDTO item : dto.getItems()) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) continue;
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new IllegalStateException("Producto no encontrado."));
            subtotal += producto.getPrecio() * item.getCantidad();
        }

        int costoDelivery = (tipo == TipoPedido.DELIVERY) ? COSTO_DELIVERY : 0;
        int total = subtotal + costoDelivery;

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDireccion(direccion);
        pedido.setTipoPedido(tipo);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setSubtotal(subtotal);
        pedido.setCostoDelivery(costoDelivery);
        pedido.setTotal(total);
        pedido.setObservaciones(dto.getObservaciones());

        pedido = pedidoRepository.save(pedido);

        for (ItemPedidoDTO item : dto.getItems()) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) continue;
            Producto producto = productoRepository.findById(item.getIdProducto()).get();

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * item.getCantidad());
            detallePedidoRepository.save(detalle);
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setMonto(total);
        pago.setNumeroOperacion("SIMULADO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pago.setEstado(EstadoPago.PAGADO);
        pagoRepository.save(pago);

        return pedido;
    }
}