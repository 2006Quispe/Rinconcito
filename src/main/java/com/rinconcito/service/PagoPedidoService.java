package com.rinconcito.service;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PagoPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final EvidenciaStorageService evidenciaStorageService;
    private final NotaVentaService notaVentaService;
    private final MesaRepository mesaRepository;

    public PagoPedidoService(
            PedidoRepository pedidoRepository,
            PagoRepository pagoRepository,
            DetallePedidoRepository detallePedidoRepository,
            EvidenciaStorageService evidenciaStorageService,
            NotaVentaService notaVentaService,
            MesaRepository mesaRepository
    ) {
        this.pedidoRepository =
                pedidoRepository;

        this.pagoRepository =
                pagoRepository;

        this.detallePedidoRepository =
                detallePedidoRepository;

        this.evidenciaStorageService =
                evidenciaStorageService;

        this.notaVentaService =
                notaVentaService;

        this.mesaRepository =
                mesaRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorCodigo(
            String codigo
    ) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        return pedidoRepository.findByCodigoPedido(
                codigo.trim()
        );
    }

    @Transactional(readOnly = true)
    public Optional<Pago> ultimoPagoDe(
            Pedido pedido
    ) {
        return pagoRepository
                .findFirstByPedidoOrderByFechaPagoDesc(
                        pedido
                );
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPendientesValidacionPedidos() {
        return pagoRepository
                .findByEstadoAndPedidoIsNotNullOrderByFechaPagoAsc(
                        EstadoPago.PENDIENTE_VALIDACION
                );
    }

    @Transactional(readOnly = true)
    public List<Pedido>
    listarPedidosPresencialesPendientesCobro() {

        List<Pedido> pedidos =
                pedidoRepository
                        .findByTipoPedidoAndEstadoInOrderByFechaRegistroAsc(
                                TipoPedido.PRESENCIAL,
                                List.of(
                                        EstadoPedido.PENDIENTE_PAGO
                                )
                        );

        pedidos.forEach(this::inicializarPedido);

        return pedidos;
    }

    @Transactional(readOnly = true)
    public List<DetallePedido> detallesDe(
            Pedido pedido
    ) {
        if (pedido == null) {
            return List.of();
        }

        List<DetallePedido> detalles =
                detallePedidoRepository
                        .findByPedido(pedido);

        detalles.forEach(detalle -> {
            if (detalle.getProducto() != null) {
                detalle.getProducto().getNombre();
                detalle.getProducto().getImagen();

                if (
                        detalle.getProducto()
                                .getCategoria() != null
                ) {
                    detalle.getProducto()
                            .getCategoria()
                            .getNombre();
                }
            }
        });

        return detalles;
    }

    @Transactional(readOnly = true)
    public Pago obtenerPagoPedido(
            Integer idPago
    ) {
        if (idPago == null) {
            throw new IllegalStateException(
                    "No se indicó el pago."
            );
        }

        Pago pago =
                pagoRepository
                        .findById(idPago)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Pago no encontrado."
                                )
                        );

        if (pago.getPedido() == null) {
            throw new IllegalStateException(
                    "Este pago no corresponde a un pedido."
            );
        }

        inicializarPedido(
                pago.getPedido()
        );

        if (pago.getUsuarioValidador() != null) {
            pago.getUsuarioValidador().getNombres();
            pago.getUsuarioValidador().getApellidos();
        }

        return pago;
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPedidoPresencialPendiente(
            Integer idPedido
    ) {
        if (idPedido == null) {
            throw new IllegalStateException(
                    "No se indicó el pedido."
            );
        }

        Pedido pedido =
                pedidoRepository
                        .findByIdPedidoAndTipoPedido(
                                idPedido,
                                TipoPedido.PRESENCIAL
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Pedido presencial no encontrado."
                                )
                        );

        if (
                pedido.getEstado()
                        != EstadoPedido.PENDIENTE_PAGO
        ) {
            throw new IllegalStateException(
                    "El pedido no está pendiente de cobro."
            );
        }

        inicializarPedido(pedido);

        return pedido;
    }

    @Transactional
    public Pago registrarEvidencia(
            String codigoPedido,
            MetodoPago metodo,
            String numeroOperacion,
            MultipartFile captura
    ) {
        validarCodigoPedido(codigoPedido);
        validarMetodoPagoDelivery(metodo);

        Pedido pedido =
                pedidoRepository
                        .findByCodigoPedido(
                                codigoPedido.trim()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encontró el pedido "
                                                + "con ese código."
                                )
                        );

        if (
                pedido.getTipoPedido()
                        == TipoPedido.PRESENCIAL
        ) {
            throw new IllegalStateException(
                    "Los pedidos presenciales se pagan "
                            + "directamente en Caja."
            );
        }

        if (
                pedido.getEstado()
                        != EstadoPedido.PENDIENTE_PAGO
        ) {
            throw new IllegalStateException(
                    "Este pedido no admite el envío "
                            + "de evidencia en su estado actual ("
                            + pedido.getEstado()
                            + ")."
            );
        }

        validarTotalPedido(pedido);

        Optional<Pago> pagoPendiente =
                pagoRepository
                        .findFirstByPedidoAndEstadoOrderByFechaPagoDesc(
                                pedido,
                                EstadoPago.PENDIENTE_VALIDACION
                        );

        if (pagoPendiente.isPresent()) {
            throw new IllegalStateException(
                    "Ya hay un pago en revisión para "
                            + "este pedido. Espera la validación."
            );
        }

        String operacion =
                validarNumeroOperacion(
                        metodo,
                        numeroOperacion
                );

        String rutaEvidencia =
                evidenciaStorageService.guardar(
                        captura,
                        pedido.getCodigoPedido()
                );

        Pago pago =
                new Pago();

        pago.setPedido(pedido);
        pago.setReserva(null);
        pago.setMetodoPago(metodo);
        pago.setMonto(pedido.getTotal());
        pago.setNumeroOperacion(operacion);
        pago.setEvidenciaPago(rutaEvidencia);
        pago.setEstado(
                EstadoPago.PENDIENTE_VALIDACION
        );

        Pago pagoGuardado =
                pagoRepository.save(pago);

        pedido.setEstado(
                EstadoPedido.PENDIENTE_VALIDACION
        );

        pedidoRepository.save(pedido);

        return pagoGuardado;
    }

    @Transactional
    public synchronized Pedido registrarPagoPresencial(
            Integer idPedido,
            MetodoPago metodoPago,
            String numeroOperacion,
            Usuario validador
    ) {
        validarValidador(validador);
        validarMetodoPagoPresencial(metodoPago);

        Pedido pedido =
                obtenerPedidoPresencialPendiente(
                        idPedido
                );

        validarTotalPedido(pedido);

        if (pedido.getMesa() == null) {
            throw new IllegalStateException(
                    "El pedido no tiene una mesa asignada."
            );
        }

        if (
                pedido.getMesa().getEstado()
                        != EstadoMesa.OCUPADA
        ) {
            throw new IllegalStateException(
                    "La mesa del pedido ya no está ocupada."
            );
        }

        Optional<Pago> ultimoPago =
                pagoRepository
                        .findFirstByPedidoOrderByFechaPagoDesc(
                                pedido
                        );

        if (ultimoPago.isPresent()) {
            EstadoPago estadoAnterior =
                    ultimoPago.get().getEstado();

            if (estadoAnterior == EstadoPago.APROBADO) {
                throw new IllegalStateException(
                        "Este pedido ya tiene un pago aprobado."
                );
            }

            if (
                    estadoAnterior
                            == EstadoPago.PENDIENTE_VALIDACION
            ) {
                throw new IllegalStateException(
                        "Este pedido ya tiene un pago pendiente."
                );
            }
        }

        String operacion =
                validarNumeroOperacion(
                        metodoPago,
                        numeroOperacion
                );

        Pago pago =
                new Pago();

        pago.setPedido(pedido);
        pago.setReserva(null);
        pago.setMetodoPago(metodoPago);
        pago.setMonto(pedido.getTotal());
        pago.setNumeroOperacion(operacion);
        pago.setEvidenciaPago(null);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setUsuarioValidador(validador);
        pago.setFechaValidacion(
                LocalDateTime.now()
        );

        pago.setObservacionValidacion(
                "Pago presencial registrado en Caja."
        );

        pago =
                pagoRepository.save(pago);

        pedido.setEstado(
                EstadoPedido.FINALIZADO
        );

        pedido =
                pedidoRepository.save(pedido);

        Mesa mesa =
                pedido.getMesa();

        mesa.setEstado(
                EstadoMesa.DISPONIBLE
        );

        mesaRepository.save(mesa);

        notaVentaService.emitir(
                pago,
                validador
        );

        inicializarPedido(pedido);

        return pedido;
    }

    @Transactional
    public Pedido aprobar(
            Integer idPago,
            Usuario validador
    ) {
        validarValidador(validador);

        Pago pago =
                obtenerPagoPedido(idPago);

        if (
                pago.getEstado()
                        != EstadoPago.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "Este pago ya fue procesado."
            );
        }

        Pedido pedido =
                pago.getPedido();

        if (
                pedido.getTipoPedido()
                        == TipoPedido.PRESENCIAL
        ) {
            throw new IllegalStateException(
                    "El pago presencial debe registrarse "
                            + "desde Cobros presenciales."
            );
        }

        if (
                pedido.getEstado()
                        != EstadoPedido.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "El pedido no está pendiente "
                            + "de validación."
            );
        }

        int montoEsperado =
                pedido.getTotal() != null
                        ? pedido.getTotal()
                        : 0;

        if (
                pago.getMonto() == null
                        || !pago.getMonto()
                        .equals(montoEsperado)
                        || montoEsperado <= 0
        ) {
            throw new IllegalStateException(
                    "El monto del pago no coincide "
                            + "con el pedido. Monto esperado: S/ "
                            + montoEsperado
                            + "."
            );
        }

        pago.setEstado(
                EstadoPago.APROBADO
        );

        pago.setUsuarioValidador(
                validador
        );

        pago.setFechaValidacion(
                LocalDateTime.now()
        );

        pago.setObservacionValidacion(null);

        pago =
                pagoRepository.save(pago);

        pedido.setEstado(
                EstadoPedido.CONFIRMADO
        );

        pedido =
                pedidoRepository.save(pedido);

        notaVentaService.emitir(
                pago,
                validador
        );

        inicializarPedido(pedido);

        return pedido;
    }

    @Transactional
    public void rechazar(
            Integer idPago,
            Usuario validador,
            String motivo
    ) {
        validarValidador(validador);

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalStateException(
                    "Debes indicar el motivo del rechazo."
            );
        }

        if (motivo.trim().length() > 300) {
            throw new IllegalStateException(
                    "El motivo no puede superar "
                            + "los 300 caracteres."
            );
        }

        Pago pago =
                obtenerPagoPedido(idPago);

        if (
                pago.getEstado()
                        != EstadoPago.PENDIENTE_VALIDACION
        ) {
            throw new IllegalStateException(
                    "Este pago ya fue procesado."
            );
        }

        Pedido pedido =
                pago.getPedido();

        if (
                pedido.getTipoPedido()
                        == TipoPedido.PRESENCIAL
        ) {
            throw new IllegalStateException(
                    "Los cobros presenciales no utilizan "
                            + "el proceso de rechazo de evidencia."
            );
        }

        pago.setEstado(
                EstadoPago.RECHAZADO
        );

        pago.setObservacionValidacion(
                motivo.trim()
        );

        pago.setUsuarioValidador(
                validador
        );

        pago.setFechaValidacion(
                LocalDateTime.now()
        );

        pagoRepository.save(pago);

        pedido.setEstado(
                EstadoPedido.PENDIENTE_PAGO
        );

        pedidoRepository.save(pedido);
    }

    private void inicializarPedido(
            Pedido pedido
    ) {
        if (pedido == null) {
            return;
        }

        pedido.getCodigoPedido();

        if (pedido.getCliente() != null) {
            pedido.getCliente().getNombres();
            pedido.getCliente().getApellidos();
            pedido.getCliente().getTelefono();
            pedido.getCliente().getDni();
            pedido.getCliente().getCorreo();
        }

        if (pedido.getMesero() != null) {
            pedido.getMesero().getNombres();
            pedido.getMesero().getApellidos();
        }

        if (pedido.getDireccion() != null) {
            pedido.getDireccion().getDireccion();
            pedido.getDireccion().getDistrito();
            pedido.getDireccion().getReferencia();
        }

        if (pedido.getMesa() != null) {
            pedido.getMesa().getNumero();

            if (pedido.getMesa().getPiso() != null) {
                pedido.getMesa()
                        .getPiso()
                        .getNombre();
            }
        }
    }

    private void validarCodigoPedido(
            String codigoPedido
    ) {
        if (
                codigoPedido == null
                        || codigoPedido.isBlank()
        ) {
            throw new IllegalStateException(
                    "Debes indicar el código del pedido."
            );
        }
    }

    private void validarTotalPedido(
            Pedido pedido
    ) {
        if (
                pedido.getTotal() == null
                        || pedido.getTotal() <= 0
        ) {
            throw new IllegalStateException(
                    "El pedido no tiene un total válido para pagar."
            );
        }
    }

    private void validarValidador(
            Usuario validador
    ) {
        if (validador == null) {
            throw new IllegalStateException(
                    "No se indicó el usuario de Caja."
            );
        }

        if (
                validador.getEstado()
                        != Usuario.EstadoUsuario.ACTIVO
        ) {
            throw new IllegalStateException(
                    "El usuario de Caja está inactivo."
            );
        }

        if (validador.getRol() == null) {
            throw new IllegalStateException(
                    "El usuario no tiene un rol asignado."
            );
        }

        String rol =
                validador.getRol()
                        .getNombre();

        if (
                !"CAJA".equalsIgnoreCase(rol)
                        && !"ADMINISTRADOR"
                        .equalsIgnoreCase(rol)
        ) {
            throw new IllegalStateException(
                    "El usuario no tiene permiso "
                            + "para registrar pagos."
            );
        }
    }

    private void validarMetodoPagoDelivery(
            MetodoPago metodo
    ) {
        if (
                metodo != MetodoPago.YAPE
                        && metodo != MetodoPago.PLIN
                        && metodo
                        != MetodoPago.TRANSFERENCIA
        ) {
            throw new IllegalStateException(
                    "Solo se acepta Yape, Plin "
                            + "o transferencia bancaria "
                            + "para pedidos delivery."
            );
        }
    }

    private void validarMetodoPagoPresencial(
            MetodoPago metodo
    ) {
        if (metodo == null) {
            throw new IllegalStateException(
                    "Debes seleccionar el método de pago."
            );
        }

        if (
                metodo != MetodoPago.EFECTIVO
                        && metodo != MetodoPago.YAPE
                        && metodo != MetodoPago.PLIN
                        && metodo
                        != MetodoPago.TRANSFERENCIA
        ) {
            throw new IllegalStateException(
                    "El método de pago seleccionado no es válido."
            );
        }
    }

    private String validarNumeroOperacion(
            MetodoPago metodo,
            String numeroOperacion
    ) {
        if (metodo == MetodoPago.EFECTIVO) {
            return null;
        }

        String operacion =
                blankToNull(numeroOperacion);

        if (operacion == null) {
            throw new IllegalStateException(
                    "Debes ingresar el número de operación."
            );
        }

        if (operacion.length() > 50) {
            throw new IllegalStateException(
                    "El número de operación no puede superar "
                            + "los 50 caracteres."
            );
        }

        if (
                pagoRepository
                        .existsByNumeroOperacion(
                                operacion
                        )
        ) {
            throw new IllegalStateException(
                    "El número de operación ya fue registrado."
            );
        }

        return operacion;
    }

    private String blankToNull(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}