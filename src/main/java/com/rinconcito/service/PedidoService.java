package com.rinconcito.service;

import com.rinconcito.dto.CarritoItemDTO;
import com.rinconcito.dto.PedidoFormDTO;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Direccion;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.repository.DireccionRepository;
import com.rinconcito.repository.PedidoRepository;
import com.rinconcito.repository.ProductoRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    public static final int COSTO_DELIVERY = 0;

    public static final String MENSAJE_TRASLADO =
            "El servicio de delivery se coordina por separado "
            + "mediante InDrive u otro servicio externo.";

    public static final String MENSAJE_RECOJO =
            "Cuando Caja confirme el pago, podrás recoger "
            + "tu pedido directamente en el restaurante.";

    /*
     * Permite letras, tildes, ñ, espacios,
     * apóstrofes y guiones.
     */
    private static final String PATRON_NOMBRE =
            "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorCodigo(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        return pedidoRepository.findByCodigoPedido(
                codigo.trim().toUpperCase()
        );
    }

    @Transactional(readOnly = true)
    public List<DetallePedido> listarDetalles(
            Pedido pedido
    ) {
        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedido(pedido);

        detalles.forEach(detalle -> {

            if (detalle.getProducto() != null) {
                detalle.getProducto().getNombre();
                detalle.getProducto().getImagen();
            }
        });

        return detalles;
    }

    @Transactional
    public Pedido crearPedido(
            PedidoFormDTO dto,
            List<CarritoItemDTO> itemsCarrito,
            Usuario clienteLogueado
    ) {
        if (dto == null) {
            throw new IllegalStateException(
                    "No se recibieron los datos del pedido."
            );
        }

        normalizarDatos(dto);

        TipoPedido tipoPedido =
                obtenerTipoPedido(dto);

        validarDatosCliente(
                dto,
                tipoPedido
        );

        if (itemsCarrito == null
                || itemsCarrito.isEmpty()) {

            throw new IllegalStateException(
                    "Tu carrito está vacío. "
                    + "Agrega productos desde la carta."
            );
        }

        List<LineaValidada> lineas =
                validarProductosDelCarrito(
                        itemsCarrito
                );

        int subtotal =
                lineas.stream()
                        .mapToInt(linea -> linea.subtotal)
                        .sum();

        int costoDelivery =
                tipoPedido == TipoPedido.DELIVERY
                        ? COSTO_DELIVERY
                        : 0;

        int total =
                subtotal + costoDelivery;

        Usuario cliente =
                resolverCliente(
                        dto,
                        clienteLogueado
                );

        Direccion direccion = null;

        if (tipoPedido == TipoPedido.DELIVERY) {

            direccion =
                    registrarDireccion(
                            dto,
                            cliente
                    );
        }

        Pedido pedido =
                new Pedido();

        pedido.setCodigoPedido(
                generarCodigoPedido()
        );

        pedido.setCliente(cliente);
        pedido.setDireccion(direccion);
        pedido.setMesa(null);
        pedido.setMesero(null);
        pedido.setTipoPedido(tipoPedido);
        pedido.setEstado(
                EstadoPedido.PENDIENTE_PAGO
        );
        pedido.setSubtotal(subtotal);
        pedido.setCostoDelivery(costoDelivery);
        pedido.setTotal(total);

        pedido.setObservaciones(
                textoOpcional(
                        dto.getObservaciones()
                )
        );

        pedido =
                pedidoRepository.save(pedido);

        registrarDetalles(
                pedido,
                lineas
        );

        pedido.getCliente().getNombres();

        if (pedido.getDireccion() != null) {
            pedido.getDireccion().getDireccion();
        }

        return pedido;
    }

    /*
     * Se conserva para las pruebas y llamadas anteriores.
     */
    @Transactional
    public Pedido crearPedidoDelivery(
            PedidoFormDTO dto,
            List<CarritoItemDTO> itemsCarrito,
            Usuario clienteLogueado
    ) {
        return crearPedido(
                dto,
                itemsCarrito,
                clienteLogueado
        );
    }

    private void normalizarDatos(
            PedidoFormDTO dto
    ) {
        dto.setTipoEntrega(
                limpiarTexto(dto.getTipoEntrega())
        );

        dto.setDni(
                limpiarTexto(dto.getDni())
        );

        dto.setNombres(
                limpiarEspacios(dto.getNombres())
        );

        dto.setApellidos(
                limpiarEspacios(dto.getApellidos())
        );

        dto.setTelefono(
                limpiarTexto(dto.getTelefono())
        );

        dto.setDireccionTexto(
                limpiarEspacios(dto.getDireccionTexto())
        );

        dto.setDistrito(
                limpiarEspacios(dto.getDistrito())
        );

        dto.setReferencia(
                limpiarEspacios(dto.getReferencia())
        );

        dto.setObservaciones(
                limpiarEspacios(dto.getObservaciones())
        );
    }

    private TipoPedido obtenerTipoPedido(
            PedidoFormDTO dto
    ) {
        String tipoEntrega =
                dto.getTipoEntrega();

        if (tipoEntrega == null
                || tipoEntrega.isBlank()) {

            return TipoPedido.DELIVERY;
        }

        String valor =
                tipoEntrega.toUpperCase();

        return switch (valor) {

            case "DELIVERY" ->
                    TipoPedido.DELIVERY;

            case "RECOJO_LOCAL" ->
                    TipoPedido.RECOJO_LOCAL;

            default ->
                    throw new IllegalStateException(
                            "Selecciona una modalidad "
                            + "de entrega válida."
                    );
        };
    }

    private void validarDatosCliente(
            PedidoFormDTO dto,
            TipoPedido tipoPedido
    ) {
        if (dto.getDni() == null
                || !dto.getDni().matches("\\d{8}")) {

            throw new IllegalStateException(
                    "El DNI debe contener exactamente 8 números."
            );
        }

        validarNombre(
                dto.getNombres(),
                "Los nombres"
        );

        validarNombre(
                dto.getApellidos(),
                "Los apellidos"
        );

        if (dto.getTelefono() == null
                || !dto.getTelefono().matches("9\\d{8}")) {

            throw new IllegalStateException(
                    "El teléfono debe contener 9 números "
                    + "y comenzar con 9."
            );
        }

        if (tipoPedido == TipoPedido.DELIVERY) {
            validarDireccionDelivery(dto);
        }

        if (dto.getObservaciones() != null
                && dto.getObservaciones().length() > 300) {

            throw new IllegalStateException(
                    "Las observaciones del pedido "
                    + "no pueden superar 300 caracteres."
            );
        }
    }

    private void validarNombre(
            String texto,
            String nombreCampo
    ) {
        if (texto == null || texto.isBlank()) {

            throw new IllegalStateException(
                    nombreCampo + " son obligatorios."
            );
        }

        if (texto.length() < 2) {

            throw new IllegalStateException(
                    nombreCampo
                    + " deben contener al menos 2 caracteres."
            );
        }

        if (texto.length() > 50) {

            throw new IllegalStateException(
                    nombreCampo
                    + " no pueden superar 50 caracteres."
            );
        }

        if (!texto.matches(PATRON_NOMBRE)) {

            throw new IllegalStateException(
                    nombreCampo
                    + " solo pueden contener letras, "
                    + "espacios, tildes, apóstrofes y guiones."
            );
        }
    }

    private void validarDireccionDelivery(
            PedidoFormDTO dto
    ) {
        String direccion =
                dto.getDireccionTexto();

        if (direccion == null
                || direccion.isBlank()) {

            throw new IllegalStateException(
                    "La dirección de entrega es obligatoria."
            );
        }

        if (direccion.length() < 5) {

            throw new IllegalStateException(
                    "La dirección debe contener "
                    + "al menos 5 caracteres."
            );
        }

        if (direccion.length() > 200) {

            throw new IllegalStateException(
                    "La dirección no puede superar "
                    + "200 caracteres."
            );
        }

        if (!direccion.matches(".*[\\p{L}\\d].*")) {

            throw new IllegalStateException(
                    "Ingresa una dirección válida."
            );
        }

        String distrito =
                dto.getDistrito();

        if (distrito == null
                || distrito.isBlank()) {

            throw new IllegalStateException(
                    "El distrito es obligatorio."
            );
        }

        if (distrito.length() < 2
                || distrito.length() > 50) {

            throw new IllegalStateException(
                    "El distrito debe contener entre "
                    + "2 y 50 caracteres."
            );
        }

        if (!distrito.matches(PATRON_NOMBRE)) {

            throw new IllegalStateException(
                    "El distrito solo puede contener letras, "
                    + "espacios, tildes y guiones."
            );
        }

        String referencia =
                dto.getReferencia();

        if (referencia == null
                || referencia.isBlank()) {

            throw new IllegalStateException(
                    "La referencia de la dirección "
                    + "es obligatoria."
            );
        }

        if (referencia.length() < 3) {

            throw new IllegalStateException(
                    "La referencia debe contener "
                    + "al menos 3 caracteres."
            );
        }

        if (referencia.length() > 200) {

            throw new IllegalStateException(
                    "La referencia no puede superar "
                    + "200 caracteres."
            );
        }
    }

    private Direccion registrarDireccion(
            PedidoFormDTO dto,
            Usuario cliente
    ) {
        Direccion direccion =
                new Direccion();

        direccion.setUsuario(cliente);
        direccion.setDireccion(
                dto.getDireccionTexto()
        );
        direccion.setDistrito(
                dto.getDistrito()
        );
        direccion.setReferencia(
                dto.getReferencia()
        );
        direccion.setPrincipal(false);

        return direccionRepository.save(
                direccion
        );
    }

    private Usuario resolverCliente(
            PedidoFormDTO dto,
            Usuario clienteLogueado
    ) {
        if (clienteLogueado != null
                && clienteLogueado.getRol() != null
                && "CLIENTE".equalsIgnoreCase(
                        clienteLogueado
                                .getRol()
                                .getNombre()
                )) {

            clienteLogueado.setTelefono(
                    dto.getTelefono()
            );

            return usuarioRepository.save(
                    clienteLogueado
            );
        }

        return usuarioService.buscarOCrearInvitado(
                dto.getDni(),
                dto.getNombres(),
                dto.getApellidos(),
                dto.getTelefono()
        );
    }

    private List<LineaValidada>
    validarProductosDelCarrito(
            List<CarritoItemDTO> itemsCarrito
    ) {
        List<LineaValidada> lineas =
                new ArrayList<>();

        for (CarritoItemDTO item : itemsCarrito) {

            if (item.getIdProducto() == null) {

                throw new IllegalStateException(
                        "Hay un producto no válido "
                        + "en el carrito."
                );
            }

            if (item.getCantidad() == null
                    || item.getCantidad() <= 0) {

                throw new IllegalStateException(
                        "La cantidad de cada producto "
                        + "debe ser mayor que cero."
                );
            }

            if (item.getCantidad() > 30) {

                throw new IllegalStateException(
                        "Solo puedes pedir hasta "
                        + "30 unidades de cada producto."
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

            int precioUnitario =
                    producto.getPrecio();

            int cantidad =
                    item.getCantidad();

            int subtotal =
                    precioUnitario * cantidad;

            lineas.add(
                    new LineaValidada(
                            producto,
                            cantidad,
                            precioUnitario,
                            subtotal
                    )
            );
        }

        if (lineas.isEmpty()) {

            throw new IllegalStateException(
                    "Debes elegir al menos un producto."
            );
        }

        return lineas;
    }

    private void validarProducto(
            Producto producto
    ) {
        if (producto.getEstado()
                != EstadoProducto.ACTIVO) {

            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" ya no está activo."
            );
        }

        if (!producto.isDisponible()) {

            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" está agotado."
            );
        }

        if (producto.getPrecio() == null
                || producto.getPrecio() <= 0) {

            throw new IllegalStateException(
                    "El producto \""
                    + producto.getNombre()
                    + "\" no tiene un precio válido."
            );
        }
    }

    private void registrarDetalles(
            Pedido pedido,
            List<LineaValidada> lineas
    ) {
        for (LineaValidada linea : lineas) {

            DetallePedido detalle =
                    new DetallePedido();

            detalle.setPedido(pedido);
            detalle.setProducto(
                    linea.producto
            );
            detalle.setCantidad(
                    linea.cantidad
            );
            detalle.setPrecioUnitario(
                    linea.precioUnitario
            );
            detalle.setSubtotal(
                    linea.subtotal
            );
            detalle.setObservaciones(null);

            detallePedidoRepository.save(
                    detalle
            );
        }
    }

    private String generarCodigoPedido() {

        int anioActual =
                LocalDate.now().getYear();

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
                    ultimoPedido
                            .get()
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

    private String limpiarTexto(
            String texto
    ) {
        if (texto == null) {
            return null;
        }

        String limpio =
                texto.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String limpiarEspacios(
            String texto
    ) {
        if (texto == null) {
            return null;
        }

        String limpio =
                texto
                        .trim()
                        .replaceAll("\\s+", " ");

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String textoOpcional(
            String texto
    ) {
        return limpiarEspacios(texto);
    }

    private static final class LineaValidada {

        private final Producto producto;
        private final int cantidad;
        private final int precioUnitario;
        private final int subtotal;

        private LineaValidada(
                Producto producto,
                int cantidad,
                int precioUnitario,
                int subtotal
        ) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.subtotal = subtotal;
        }
    }
}