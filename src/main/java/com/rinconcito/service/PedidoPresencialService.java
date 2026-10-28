package com.rinconcito.service;

import com.rinconcito.dto.ItemPedidoPresencialDTO;
import com.rinconcito.dto.PedidoPresencialFormDTO;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.PedidoRepository;
import com.rinconcito.repository.ProductoRepository;
import com.rinconcito.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoPresencialService {
    @org.springframework.beans.factory.annotation.Autowired
    private com.rinconcito.service.correo.ColaCorreoService colaCorreoService;

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final int MAX_CANTIDAD_PRODUCTO = 30;
    private static final int MAX_OBSERVACION_ITEM = 200;
    private static final int MAX_OBSERVACION_PEDIDO = 300;

    private static final List<EstadoPedido>
            ESTADOS_PEDIDO_ACTIVO =
            List.of(
                    EstadoPedido.PENDIENTE_PAGO,
                    EstadoPedido.PENDIENTE_VALIDACION,
                    EstadoPedido.CONFIRMADO
            );

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;

    public PedidoPresencialService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            ProductoRepository productoRepository,
            MesaRepository mesaRepository,
            ReservaRepository reservaRepository,
            UsuarioService usuarioService
    ) {
        this.pedidoRepository =
                pedidoRepository;

        this.detallePedidoRepository =
                detallePedidoRepository;

        this.productoRepository =
                productoRepository;

        this.mesaRepository =
                mesaRepository;

        this.reservaRepository =
                reservaRepository;

        this.usuarioService =
                usuarioService;
    }

    @Transactional(readOnly = true)
    public List<Mesa> listarMesasOcupadas() {
        List<Mesa> mesas =
                mesaRepository
                        .findAllByOrderByPisoIdPisoAscNumeroAsc();

        return mesas.stream()
                .filter(mesa ->
                        mesa.getEstado()
                                == EstadoMesa.OCUPADA
                )
                .peek(mesa -> {
                    if (mesa.getPiso() != null) {
                        mesa.getPiso().getNombre();
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosDisponibles() {
        List<Producto> productos =
                productoRepository.buscarPublicos(
                        EstadoProducto.ACTIVO,
                        EstadoCategoria.ACTIVO,
                        null,
                        null
                );

        return productos.stream()
                .filter(Producto::isDisponible)
                .toList();
    }

    /*
     * Registra un pedido presencial.
     *
     * Si el pedido proviene de una reserva,
     * utiliza al cliente de esa reserva.
     *
     * Si no proviene de una reserva, utiliza
     * al usuario Consumidor Final.
     */
    @Transactional
    public synchronized Pedido registrarPedido(
            PedidoPresencialFormDTO formulario,
            Usuario mesero
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos del pedido."
            );
        }

        validarMesero(mesero);

        Mesa mesa =
                obtenerMesa(
                        formulario.getIdMesa()
                );

        validarMesa(mesa);
        validarMesaSinPedidoActivo(mesa);

        String observacionGeneral =
                validarObservacionGeneral(
                        formulario.getObservaciones()
                );

        List<LineaPedido> lineas =
                validarLineas(
                        formulario.getItems()
                );

        if (lineas.isEmpty()) {
            throw new IllegalStateException(
                    "Debes agregar al menos un producto."
            );
        }

        int subtotal =
                calcularSubtotal(lineas);

        Usuario cliente =
                obtenerClienteDelPedido(
                        formulario,
                        mesa
                );

        Pedido pedido =
                new Pedido();

        pedido.setCodigoPedido(
                generarCodigoPedido()
        );

        pedido.setCliente(cliente);
        pedido.setMesero(mesero);
        pedido.setMesa(mesa);
        pedido.setDireccion(null);
        pedido.setTipoPedido(
                TipoPedido.PRESENCIAL
        );

        pedido.setEstado(
                EstadoPedido.CONFIRMADO
        );

        pedido.setSubtotal(subtotal);
        pedido.setCostoDelivery(0);
        pedido.setTotal(subtotal);
        pedido.setObservaciones(
                observacionGeneral
        );

        pedido =
                pedidoRepository.save(pedido);

        guardarDetalles(
                pedido,
                lineas
        );

        colaCorreoService.pedido(pedido, cliente.getCorreo());
        return pedido;
    }

    /*
     * Agrega productos nuevos a un pedido
     * presencial que todavía está en atención.
     */
    @Transactional
    public synchronized Pedido agregarProductos(
            Integer idPedido,
            PedidoPresencialFormDTO formulario,
            Usuario mesero
    ) {
        if (formulario == null) {
            throw new IllegalStateException(
                    "No se recibieron los productos."
            );
        }

        validarMesero(mesero);

        Pedido pedido =
                obtenerPedidoInterno(idPedido);

        validarPedidoModificable(pedido);

        List<LineaPedido> lineas =
                validarLineas(
                        formulario.getItems()
                );

        if (lineas.isEmpty()) {
            throw new IllegalStateException(
                    "Debes agregar al menos un producto."
            );
        }

        int montoAgregado =
                calcularSubtotal(lineas);

        int subtotalActual =
                pedido.getSubtotal() != null
                        ? pedido.getSubtotal()
                        : 0;

        int nuevoSubtotal =
                subtotalActual
                        + montoAgregado;

        pedido.setSubtotal(nuevoSubtotal);
        pedido.setCostoDelivery(0);
        pedido.setTotal(nuevoSubtotal);

        agregarObservacionGeneral(
                pedido,
                formulario.getObservaciones()
        );

        pedido =
                pedidoRepository.save(pedido);

        guardarDetalles(
                pedido,
                lineas
        );

        return pedido;
    }

    @Transactional
    public Pedido solicitarCobro(
            Integer idPedido,
            Usuario mesero
    ) {
        validarMesero(mesero);

        Pedido pedido =
                obtenerPedidoInterno(idPedido);

        if (
                pedido.getEstado()
                        != EstadoPedido.CONFIRMADO
        ) {
            throw new IllegalStateException(
                    "Solo se puede solicitar el cobro "
                    + "de un pedido confirmado."
            );
        }

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
                    "La mesa del pedido ya no se encuentra ocupada."
            );
        }

        if (
                pedido.getTotal() == null
                        || pedido.getTotal() <= 0
        ) {
            throw new IllegalStateException(
                    "El pedido no tiene un total válido para cobrar."
            );
        }

        pedido.setEstado(
                EstadoPedido.PENDIENTE_PAGO
        );

        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosActivos() {
        return pedidoRepository
                .findByTipoPedidoAndEstadoInOrderByFechaRegistroAsc(
                        TipoPedido.PRESENCIAL,
                        ESTADOS_PEDIDO_ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPedido(
            Integer idPedido
    ) {
        Pedido pedido =
                obtenerPedidoInterno(idPedido);

        inicializarPedido(pedido);

        return pedido;
    }

    @Transactional(readOnly = true)
    public List<DetallePedido> listarDetalles(
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
                detalle.getProducto()
                        .getNombre();

                detalle.getProducto()
                        .getImagen();

                if (
                        detalle.getProducto()
                                .getCategoria()
                                != null
                ) {
                    detalle.getProducto()
                            .getCategoria()
                            .getNombre();
                }
            }
        });

        return detalles;
    }

    /*
     * Determina qué cliente se asignará al pedido.
     */
    private Usuario obtenerClienteDelPedido(
            PedidoPresencialFormDTO formulario,
            Mesa mesa
    ) {
        Integer idReserva =
                formulario.getIdReserva();

        /*
         * Si no viene de una reserva, se trata
         * como una atención presencial normal.
         */
        if (idReserva == null) {
            return usuarioService
                    .obtenerOCrearConsumidorFinal();
        }

        Reserva reserva =
                reservaRepository
                        .buscarPorIdParaMesero(
                                idReserva
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "La reserva seleccionada "
                                        + "no fue encontrada."
                                )
                        );

        validarReservaParaPedido(
                reserva,
                mesa
        );

        return reserva.getCliente();
    }

    /*
     * Verifica que la reserva corresponda a la
     * fecha, mesa y atención actuales.
     */
    private void validarReservaParaPedido(
            Reserva reserva,
            Mesa mesa
    ) {
        LocalDate hoy =
                LocalDate.now(ZONA_PERU);

        if (
                reserva.getFechaReserva() == null
                        || !reserva.getFechaReserva()
                                .equals(hoy)
        ) {
            throw new IllegalStateException(
                    "La reserva no corresponde "
                    + "a la fecha actual."
            );
        }

        if (
                reserva.getEstado()
                        != EstadoReserva.EN_ATENCION
        ) {
            throw new IllegalStateException(
                    "La reserva debe encontrarse "
                    + "en atención antes de registrar "
                    + "su pedido."
            );
        }

        if (reserva.getMesa() == null) {
            throw new IllegalStateException(
                    "La reserva no tiene una mesa asignada."
            );
        }

        if (
                !reserva.getMesa()
                        .getIdMesa()
                        .equals(mesa.getIdMesa())
        ) {
            throw new IllegalStateException(
                    "La mesa seleccionada no corresponde "
                    + "a la mesa asignada a la reserva."
            );
        }

        if (reserva.getCliente() == null) {
            throw new IllegalStateException(
                    "La reserva no tiene un cliente asociado."
            );
        }
    }

    private Pedido obtenerPedidoInterno(
            Integer idPedido
    ) {
        if (idPedido == null) {
            throw new IllegalStateException(
                    "El identificador del pedido es obligatorio."
            );
        }

        return pedidoRepository
                .findByIdPedidoAndTipoPedido(
                        idPedido,
                        TipoPedido.PRESENCIAL
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Pedido presencial no encontrado."
                        )
                );
    }

    private void inicializarPedido(
            Pedido pedido
    ) {
        if (pedido.getCliente() != null) {
            pedido.getCliente()
                    .getNombres();

            pedido.getCliente()
                    .getApellidos();

            pedido.getCliente()
                    .getDni();
        }

        if (pedido.getMesero() != null) {
            pedido.getMesero()
                    .getNombres();

            pedido.getMesero()
                    .getApellidos();
        }

        if (pedido.getMesa() != null) {
            pedido.getMesa()
                    .getNumero();

            if (
                    pedido.getMesa()
                            .getPiso()
                            != null
            ) {
                pedido.getMesa()
                        .getPiso()
                        .getNombre();
            }
        }
    }

    private void validarPedidoModificable(
            Pedido pedido
    ) {
        if (
                pedido.getEstado()
                        != EstadoPedido.CONFIRMADO
        ) {
            throw new IllegalStateException(
                    "Solo se pueden agregar productos "
                    + "cuando el pedido está confirmado "
                    + "y todavía se encuentra en atención."
            );
        }

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
                    "La mesa del pedido ya no se encuentra ocupada."
            );
        }
    }

    private List<LineaPedido> validarLineas(
            List<ItemPedidoPresencialDTO> items
    ) {
        List<LineaPedido> lineas =
                new ArrayList<>();

        if (items == null) {
            return lineas;
        }

        for (ItemPedidoPresencialDTO item : items) {
            if (item == null) {
                continue;
            }

            Integer cantidad =
                    item.getCantidad();

            if (
                    cantidad == null
                            || cantidad == 0
            ) {
                continue;
            }

            if (cantidad < 0) {
                throw new IllegalStateException(
                        "La cantidad no puede ser negativa."
                );
            }

            if (
                    cantidad
                            > MAX_CANTIDAD_PRODUCTO
            ) {
                throw new IllegalStateException(
                        "Solo se permiten hasta "
                        + MAX_CANTIDAD_PRODUCTO
                        + " unidades de cada producto."
                );
            }

            if (item.getIdProducto() == null) {
                throw new IllegalStateException(
                        "Hay un producto no válido."
                );
            }

            Producto producto =
                    productoRepository
                            .findById(
                                    item.getIdProducto()
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Uno de los productos "
                                            + "ya no existe."
                                    )
                            );

            validarProducto(producto);

            String observacion =
                    limpiarEspacios(
                            item.getObservaciones()
                    );

            if (
                    observacion != null
                            && observacion.length()
                            > MAX_OBSERVACION_ITEM
            ) {
                throw new IllegalStateException(
                        "La observación del producto \""
                        + producto.getNombre()
                        + "\" no puede superar "
                        + MAX_OBSERVACION_ITEM
                        + " caracteres."
                );
            }

            int precioUnitario =
                    producto.getPrecio();

            int subtotal =
                    precioUnitario
                            * cantidad;

            lineas.add(
                    new LineaPedido(
                            producto,
                            cantidad,
                            precioUnitario,
                            subtotal,
                            observacion
                    )
            );
        }

        return lineas;
    }

    private int calcularSubtotal(
            List<LineaPedido> lineas
    ) {
        return lineas.stream()
                .mapToInt(
                        LineaPedido::getSubtotal
                )
                .sum();
    }

    private String validarObservacionGeneral(
            String observacion
    ) {
        String observacionLimpia =
                limpiarEspacios(observacion);

        if (
                observacionLimpia != null
                        && observacionLimpia.length()
                        > MAX_OBSERVACION_PEDIDO
        ) {
            throw new IllegalStateException(
                    "Las observaciones generales no pueden "
                    + "superar "
                    + MAX_OBSERVACION_PEDIDO
                    + " caracteres."
            );
        }

        return observacionLimpia;
    }

    private void agregarObservacionGeneral(
            Pedido pedido,
            String nuevaObservacion
    ) {
        String observacionNueva =
                validarObservacionGeneral(
                        nuevaObservacion
                );

        if (observacionNueva == null) {
            return;
        }

        String observacionActual =
                limpiarEspacios(
                        pedido.getObservaciones()
                );

        String observacionFinal;

        if (observacionActual == null) {
            observacionFinal =
                    observacionNueva;
        } else {
            observacionFinal =
                    observacionActual
                    + " | "
                    + observacionNueva;
        }

        if (
                observacionFinal.length()
                        > MAX_OBSERVACION_PEDIDO
        ) {
            throw new IllegalStateException(
                    "La suma de las observaciones del pedido "
                    + "no puede superar "
                    + MAX_OBSERVACION_PEDIDO
                    + " caracteres."
            );
        }

        pedido.setObservaciones(
                observacionFinal
        );
    }

    private void validarProducto(
            Producto producto
    ) {
        if (
                producto.getEstado()
                        != EstadoProducto.ACTIVO
        ) {
            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" está inactivo."
            );
        }

        if (!producto.isDisponible()) {
            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" está agotado."
            );
        }

        if (
                producto.getPrecio() == null
                        || producto.getPrecio() <= 0
        ) {
            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" no tiene un precio válido."
            );
        }
    }

    private void guardarDetalles(
            Pedido pedido,
            List<LineaPedido> lineas
    ) {
        for (LineaPedido linea : lineas) {
            DetallePedido detalle =
                    new DetallePedido();

            detalle.setPedido(pedido);

            detalle.setProducto(
                    linea.getProducto()
            );

            detalle.setCantidad(
                    linea.getCantidad()
            );

            detalle.setPrecioUnitario(
                    linea.getPrecioUnitario()
            );

            detalle.setSubtotal(
                    linea.getSubtotal()
            );

            detalle.setObservaciones(
                    linea.getObservaciones()
            );

            detallePedidoRepository.save(
                    detalle
            );
        }
    }

    private Mesa obtenerMesa(
            Integer idMesa
    ) {
        if (idMesa == null) {
            throw new IllegalStateException(
                    "Debes seleccionar una mesa."
            );
        }

        return mesaRepository
                .findById(idMesa)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "La mesa seleccionada no existe."
                        )
                );
    }

    private void validarMesa(
            Mesa mesa
    ) {
        if (
                mesa.getEstado()
                        == EstadoMesa.INACTIVA
        ) {
            throw new IllegalStateException(
                    "La mesa está inactiva."
            );
        }

        if (
                mesa.getEstado()
                        != EstadoMesa.OCUPADA
        ) {
            throw new IllegalStateException(
                    "La mesa debe estar ocupada antes "
                    + "de registrar el pedido."
            );
        }
    }

    private void validarMesaSinPedidoActivo(
            Mesa mesa
    ) {
        boolean tienePedidoActivo =
                pedidoRepository
                        .existsByMesaAndTipoPedidoAndEstadoIn(
                                mesa,
                                TipoPedido.PRESENCIAL,
                                ESTADOS_PEDIDO_ACTIVO
                        );

        if (tienePedidoActivo) {
            throw new IllegalStateException(
                    "La mesa ya tiene un pedido activo. "
                    + "Ingresa a Pedidos activos "
                    + "para administrarlo."
            );
        }
    }

    private void validarMesero(
            Usuario mesero
    ) {
        if (mesero == null) {
            throw new IllegalStateException(
                    "No se pudo identificar al mesero."
            );
        }

        if (
                mesero.getRol() == null
                        || !"MESERO".equalsIgnoreCase(
                                mesero.getRol()
                                        .getNombre()
                        )
        ) {
            throw new IllegalStateException(
                    "El usuario autenticado no tiene "
                    + "el rol MESERO."
            );
        }

        if (
                mesero.getEstado()
                        != Usuario.EstadoUsuario.ACTIVO
        ) {
            throw new IllegalStateException(
                    "El usuario mesero está inactivo."
            );
        }
    }

    private String generarCodigoPedido() {
        int anioActual =
                LocalDate.now(ZONA_PERU)
                        .getYear();

        String prefijo =
                "PED-" + anioActual + "-";

        Optional<Pedido> ultimoPedido =
                pedidoRepository
                        .findTopByCodigoPedidoStartingWithOrderByCodigoPedidoDesc(
                                prefijo
                        );

        int siguienteNumero = 1;

        if (ultimoPedido.isPresent()) {
            String codigoAnterior =
                    ultimoPedido.get()
                            .getCodigoPedido();

            String parteNumerica =
                    codigoAnterior.substring(
                            prefijo.length()
                    );

            try {
                siguienteNumero =
                        Integer.parseInt(
                                parteNumerica
                        ) + 1;

            } catch (NumberFormatException ignored) {
                siguienteNumero = 1;
            }
        }

        return prefijo
                + String.format(
                        "%06d",
                        siguienteNumero
                );
    }

    private String limpiarEspacios(
            String texto
    ) {
        if (texto == null) {
            return null;
        }

        String limpio =
                texto.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private static final class LineaPedido {

        private final Producto producto;
        private final int cantidad;
        private final int precioUnitario;
        private final int subtotal;
        private final String observaciones;

        private LineaPedido(
                Producto producto,
                int cantidad,
                int precioUnitario,
                int subtotal,
                String observaciones
        ) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.subtotal = subtotal;
            this.observaciones = observaciones;
        }

        public Producto getProducto() {
            return producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public int getPrecioUnitario() {
            return precioUnitario;
        }

        public int getSubtotal() {
            return subtotal;
        }

        public String getObservaciones() {
            return observaciones;
        }
    }
}