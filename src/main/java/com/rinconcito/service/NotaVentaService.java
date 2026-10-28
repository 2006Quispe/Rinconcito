package com.rinconcito.service;

import com.rinconcito.service.correo.ColaCorreoService;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.NotaVenta.EstadoNotaVenta;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.NotaVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class NotaVentaService {

    @org.springframework.beans.factory.annotation.Autowired
    private ColaCorreoService colaCorreoService;

    private static final String SERIE_NOTA =
            "NV01";

    private final NotaVentaRepository notaVentaRepository;

    private final DetallePedidoRepository
            detallePedidoRepository;

    public NotaVentaService(
            NotaVentaRepository notaVentaRepository,
            DetallePedidoRepository detallePedidoRepository
    ) {
        this.notaVentaRepository =
                notaVentaRepository;

        this.detallePedidoRepository =
                detallePedidoRepository;
    }

    @Transactional
    public synchronized NotaVenta emitir(
            Pago pago,
            Usuario usuarioEmisor
    ) {
        if (
                pago == null
                        || pago.getIdPago() == null
        ) {
            throw new IllegalStateException(
                    "El pago no es válido."
            );
        }

        if (usuarioEmisor == null) {
            throw new IllegalStateException(
                    "El usuario emisor es obligatorio."
            );
        }

        if (
                pago.getPedido() == null
                        && pago.getReserva() == null
        ) {
            throw new IllegalStateException(
                    "El pago no pertenece a un pedido ni a una reserva."
            );
        }

        return notaVentaRepository
                .findByPago(pago)
                .orElseGet(() ->
                        crearNotaVenta(
                                pago,
                                usuarioEmisor
                        )
                );
    }

    private NotaVenta crearNotaVenta(
            Pago pago,
            Usuario usuarioEmisor
    ) {
        Usuario cliente;
        Integer total;

        if (pago.getPedido() != null) {

            Pedido pedido =
                    pago.getPedido();

            cliente =
                    pedido.getCliente();

            total =
                    pago.getMonto() != null
                            ? pago.getMonto()
                            : pedido.getTotal();

        } else {

            Reserva reserva =
                    pago.getReserva();

            cliente =
                    reserva.getCliente();

            total =
                    pago.getMonto() != null
                            ? pago.getMonto()
                            : reserva.getMontoReserva();
        }

        if (cliente == null) {
            throw new IllegalStateException(
                    "El pago no tiene un cliente asociado."
            );
        }

        Integer siguienteNumero =
                notaVentaRepository
                        .findFirstBySerieOrderByNumeroDesc(
                                SERIE_NOTA
                        )
                        .map(nota ->
                                nota.getNumero() + 1
                        )
                        .orElse(1);

        if (total == null) {
            total = 0;
        }

        NotaVenta notaVenta =
                new NotaVenta();

        notaVenta.setPago(pago);
        notaVenta.setUsuarioEmisor(
                usuarioEmisor
        );
        notaVenta.setSerie(
                SERIE_NOTA
        );
        notaVenta.setNumero(
                siguienteNumero
        );
        notaVenta.setClienteNombre(
                construirNombreCompleto(cliente)
        );
        notaVenta.setClienteDocumento(
                cliente.getDni()
        );
        notaVenta.setTotal(total);
        notaVenta.setEstado(
                EstadoNotaVenta.EMITIDA
        );

        notaVenta = notaVentaRepository.save(notaVenta);
        colaCorreoService.nota(notaVenta, false);
        return notaVenta;
    }

    @Transactional(readOnly = true)
    public List<NotaVenta> listarParaCaja() {
    	return notaVentaRepository
    	        .listarNotasParaCaja();
    }

    @Transactional(readOnly = true)
    public NotaVenta obtenerParaPdf(
            Integer idNota
    ) {
        if (idNota == null) {
            throw new IllegalStateException(
                    "El identificador de la nota es obligatorio."
            );
        }

        return notaVentaRepository
                .buscarPorIdConDatos(idNota)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Nota de venta no encontrada."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<DetallePedido> obtenerDetalles(
            NotaVenta notaVenta
    ) {
        if (
                notaVenta == null
                        || notaVenta.getPago() == null
        ) {
            throw new IllegalStateException(
                    "La nota de venta no tiene un pago asociado."
            );
        }

        /*
         * Las reservas no tienen detalles de productos.
         * Su PDF mostrará la información de la reserva.
         */
        if (
                notaVenta.getPago().getPedido() == null
                        && notaVenta.getPago().getReserva() != null
        ) {
            return Collections.emptyList();
        }

        if (
                notaVenta.getPago().getPedido() == null
        ) {
            throw new IllegalStateException(
                    "La nota no tiene pedido ni reserva asociados."
            );
        }

        Pedido pedido =
                notaVenta.getPago().getPedido();

        List<DetallePedido> detalles =
                detallePedidoRepository
                        .findByPedido(pedido);

        detalles.forEach(detalle -> {
            if (detalle.getProducto() != null) {
                detalle.getProducto().getNombre();
            }
        });

        return detalles;
    }

    public String formarNumero(
            NotaVenta notaVenta
    ) {
        if (
                notaVenta == null
                        || notaVenta.getNumero() == null
        ) {
            return "-";
        }

        String serie =
                notaVenta.getSerie() != null
                        && !notaVenta.getSerie().isBlank()
                        ? notaVenta.getSerie()
                        : SERIE_NOTA;

        return serie
                + "-"
                + String.format(
                        "%06d",
                        notaVenta.getNumero()
                );
    }

    @Transactional
    public NotaVenta anular(
            Integer idNota,
            String motivo
    ) {
        NotaVenta notaVenta =
                notaVentaRepository
                        .findById(idNota)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Nota de venta no encontrada."
                                )
                        );

        if (
                notaVenta.getEstado()
                        == EstadoNotaVenta.ANULADA
        ) {
            throw new IllegalStateException(
                    "La nota de venta ya está anulada."
            );
        }

        if (
                motivo == null
                        || motivo.isBlank()
        ) {
            throw new IllegalStateException(
                    "Debes ingresar el motivo de anulación."
            );
        }

        if (motivo.trim().length() > 300) {
            throw new IllegalStateException(
                    "El motivo no puede superar 300 caracteres."
            );
        }

        notaVenta.setEstado(
                EstadoNotaVenta.ANULADA
        );
        notaVenta.setMotivoAnulacion(
                motivo.trim()
        );
        notaVenta.setFechaAnulacion(
                LocalDateTime.now()
        );

        return notaVentaRepository.save(
                notaVenta
        );
    }

    private String construirNombreCompleto(
            Usuario usuario
    ) {
        String nombres =
                usuario.getNombres() != null
                        ? usuario.getNombres().trim()
                        : "";

        String apellidos =
                usuario.getApellidos() != null
                        ? usuario.getApellidos().trim()
                        : "";

        String nombreCompleto =
                (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank()
                ? "Cliente"
                : nombreCompleto;
    }
}