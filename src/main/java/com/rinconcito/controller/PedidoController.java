package com.rinconcito.controller;

import com.rinconcito.dto.PedidoFormDTO;
import com.rinconcito.service.ReservaService.MetodoDemo;
import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.CarritoService;
import com.rinconcito.service.ConfiguracionRestauranteService;
import com.rinconcito.service.PagoPedidoService;
import com.rinconcito.service.PedidoService;
import com.rinconcito.service.correo.ColaCorreoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PedidoController {

    private static final String PEDIDO_DEMO = "pedidoPagoDemo";

    @Value("${app.reservas.pago-demo.habilitado:${APP_RESERVAS_PAGO_DEMO_HABILITADO:true}}")
    private boolean pagoDemoHabilitado;

    private record CheckoutDemo(String codigo, String destinatario) {}

    @Autowired
    private ColaCorreoService colaCorreoService;

    @Value("${app.correo.habilitado:false}")
    private boolean correoHabilitado;

    @ModelAttribute("pagoDemoHabilitado")
    public boolean pagoDemoHabilitado() {
        return pagoDemoHabilitado;
    }

    @ModelAttribute("correoHabilitado")
    public boolean correoHabilitado() {
        return correoHabilitado;
    }


    private final PedidoService pedidoService;
    private final PagoPedidoService pagoPedidoService;
    private final CarritoService carritoService;
    private final ConfiguracionRestauranteService
            configuracionRestauranteService;
    private final UsuarioRepository usuarioRepository;

    public PedidoController(
            PedidoService pedidoService,
            PagoPedidoService pagoPedidoService,
            CarritoService carritoService,
            ConfiguracionRestauranteService
                    configuracionRestauranteService,
            UsuarioRepository usuarioRepository
    ) {
        this.pedidoService = pedidoService;
        this.pagoPedidoService = pagoPedidoService;
        this.carritoService = carritoService;
        this.configuracionRestauranteService =
                configuracionRestauranteService;
        this.usuarioRepository = usuarioRepository;
    }

    /*
     * Muestra el formulario para seleccionar
     * Delivery o Recojo en local.
     */
    @GetMapping("/pedidos")
    public String checkout(
            Model model,
            HttpSession session,
            Authentication authentication
    ) {
        cargarDatosCheckout(
                model,
                session,
                authentication
        );

        if (carritoService.estaVacio(session)) {
            model.addAttribute(
                    "carritoVacio",
                    true
            );

            return "paginas/pedidos";
        }

        PedidoFormDTO formulario =
                new PedidoFormDTO();

        prellenarSiCliente(
                formulario,
                authentication
        );

        model.addAttribute(
                "pedidoForm",
                formulario
        );

        return "paginas/pedidos";
    }

    /*
     * Registra el pedido y redirige
     * a la pantalla de pago.
     */
    @PostMapping("/pedidos")
    public String registrar(
            @ModelAttribute("pedidoForm")
            PedidoFormDTO pedidoForm,
            Model model,
            HttpSession session,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        cargarDatosCheckout(
                model,
                session,
                authentication
        );

        model.addAttribute(
                "pedidoForm",
                pedidoForm
        );

        if (carritoService.estaVacio(session)) {
            model.addAttribute(
                    "carritoVacio",
                    true
            );

            model.addAttribute(
                    "error",
                    "Tu carrito está vacío."
            );

            return "paginas/pedidos";
        }

        try {
            Usuario cliente =
                    obtenerClienteLogueado(
                            authentication
                    );

            String destinatario = pedidoForm.getCorreo();
            if (cliente != null && (destinatario == null || destinatario.isBlank())) {
                destinatario = cliente.getCorreo();
            }
            destinatario = destinatario == null ? null : destinatario.trim();
            if (!ColaCorreoService.correoValido(destinatario)) {
                throw new IllegalStateException("Ingresa un correo electrónico válido para recibir tu confirmación.");
            }

            pedidoForm.setCorreo(destinatario);
            Pedido pedido = pedidoService.crearPedido(
                    pedidoForm, carritoService.obtenerItems(session), cliente, pagoDemoHabilitado);

            if (pagoDemoHabilitado) {
                session.setAttribute(PEDIDO_DEMO, new CheckoutDemo(pedido.getCodigoPedido(), destinatario));
                carritoService.vaciar(session);
                return "redirect:/pedidos/pago-demo";
            }

            redirectAttributes.addFlashAttribute(
                    "exito",
                    mensajePedidoRegistrado(pedido)
            );

            return "redirect:/pedidos/pago?codigo="
                    + pedido.getCodigoPedido();

        } catch (IllegalStateException e) {
            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "paginas/pedidos";
        }
    }

    /*
     * Muestra la pantalla de pago.
     */
    @Autowired
    private com.rinconcito.service.UbicacionEntregaService ubicaciones;

    @PostMapping("/pedidos/ubicar-entrega")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> ubicarEntrega(
            @RequestParam(required = false) Double latitud,
            @RequestParam(required = false) Double longitud,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String distrito) {
        try {
            return org.springframework.http.ResponseEntity.ok(
                    ubicaciones.buscar(latitud, longitud, direccion, distrito));
        } catch (IllegalStateException error) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", error.getMessage()));
        }
    }

    @PostMapping("/pedidos/cotizar-delivery")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> cotizarDelivery(
            @RequestParam String distrito, @RequestParam Double latitud, @RequestParam Double longitud) {
        try {
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "costo", pedidoService.cotizarDelivery(distrito, latitud, longitud)));
        } catch (IllegalStateException error) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", error.getMessage()));
        }
    }

    @GetMapping("/pedidos/pago-demo")
    public String mostrarPagoDemo(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Pago del pedido");
        if (pagoDemoHabilitado && sesion.getAttribute(PEDIDO_DEMO) instanceof CheckoutDemo checkout) {
            pagoPedidoService.buscarPorCodigo(checkout.codigo()).ifPresent(pedido -> {
                modelo.addAttribute("pedido", pedido);
                modelo.addAttribute("puedePagar", pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO);
            });
        }
        return "paginas/pedidos/pago-demo";
    }

    @PostMapping("/pedidos/pago-demo")
    public String procesarPagoDemo(@RequestParam String codigo, @RequestParam MetodoDemo metodo,
                                   HttpSession sesion, RedirectAttributes redirect) {
        if (!pagoDemoHabilitado || !(sesion.getAttribute(PEDIDO_DEMO) instanceof CheckoutDemo checkout)
                || !checkout.codigo().equals(codigo)) {
            redirect.addFlashAttribute("error", "Abre el pedido desde el carrito para iniciar el pago de prueba.");
            return "redirect:/pedidos";
        }
        try {
            Pedido confirmado = pagoPedidoService.confirmarPagoDemo(codigo, checkout.destinatario(), metodo);
            sesion.setAttribute("ultimoPedidoConfirmado", confirmado.getCodigoPedido());
            redirect.addFlashAttribute("exito", "Pedido confirmado con pago simulado.");
            redirect.addAttribute("codigo", codigo);
            return "redirect:/pedidos/consultar";
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/pago-demo";
        }
    }

    @GetMapping("/pedidos/pago")
    public String mostrarPago(
            @RequestParam(
                    value = "codigo",
                    required = false
            )
            String codigo,
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Pagar pedido"
        );

        if (codigo == null || codigo.isBlank()) {
            model.addAttribute(
                    "error",
                    "Indica el código de tu pedido."
            );

            return "paginas/pedidos/pago";
        }

        return pagoPedidoService
                .buscarPorCodigo(codigo.trim())
                .map(pedido -> {

                    inicializarPedido(pedido);
                    model.addAttribute("estadoCorreo",
                            colaCorreoService.estadoConfirmacion("PEDIDO", pedido.getIdPedido()));

                    model.addAttribute(
                            "pedido",
                            pedido
                    );

                    model.addAttribute(
                            "detalles",
                            pagoPedidoService.detallesDe(
                                    pedido
                            )
                    );

                    pagoPedidoService
                            .ultimoPagoDe(pedido)
                            .ifPresent(pago ->
                                    model.addAttribute(
                                            "pago",
                                            pago
                                    )
                            );

                    ConfiguracionRestaurante configuracion =
                            configuracionRestauranteService
                                    .obtener();

                    model.addAttribute(
                            "configPago",
                            configuracion
                    );

                    model.addAttribute(
                            "mensajeTraslado",
                            mensajeEntrega(pedido)
                    );

                    if (
                            pedido.getEstado()
                                    != EstadoPedido.PENDIENTE_PAGO
                    ) {
                        model.addAttribute(
                                "info",
                                mensajeEstadoNoPago(
                                        pedido
                                )
                        );
                    }

                    return "paginas/pedidos/pago";
                })
                .orElseGet(() -> {
                    model.addAttribute(
                            "error",
                            "No se encontró un pedido "
                            + "con ese código."
                    );

                    return "paginas/pedidos/pago";
                });
    }

    /*
     * Recibe la captura del pago.
     */
    @PostMapping("/pedidos/pago")
    public String enviarEvidencia(
            @RequestParam("codigo")
            String codigo,

            @RequestParam("metodoPago")
            String metodoPago,

            @RequestParam(
                    value = "numeroOperacion",
                    required = false
            )
            String numeroOperacion,

            @RequestParam("captura")
            MultipartFile captura,

            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String codigoLimpio =
                codigo != null
                        ? codigo.trim()
                        : "";

        try {
            MetodoPago metodo =
                    MetodoPago.valueOf(
                            metodoPago
                                    .trim()
                                    .toUpperCase()
                    );

            pagoPedidoService.registrarEvidencia(
                    codigoLimpio,
                    metodo,
                    numeroOperacion,
                    captura
            );

            /*
             * El carrito se vacía únicamente después
             * de enviar correctamente la evidencia.
             */
            carritoService.vaciar(session);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Evidencia enviada correctamente. "
                    + "Caja revisará el pago."
            );

            return "redirect:/pedidos/consultar?codigo="
                    + codigoLimpio;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Método de pago inválido. "
                    + "Elige Yape, Plin o "
                    + "transferencia bancaria."
            );

            return "redirect:/pedidos/pago?codigo="
                    + codigoLimpio;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/pedidos/pago?codigo="
                    + codigoLimpio;
        }
    }

    /*
     * Consulta del estado del pedido.
     * La ruta se conserva para los enlaces
     * desde Mi cuenta.
     */
    @Autowired
    private com.rinconcito.repository.PedidoRepository pedidos;

    @GetMapping("/pedidos/seguimiento/{token}")
    public String seguimiento(@org.springframework.web.bind.annotation.PathVariable String token, Model modelo,
                              jakarta.servlet.http.HttpServletResponse respuesta) {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        Pedido pedido = pedidos.findByTokenSeguimiento(token)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        modelo.addAttribute("pedido", pedido);
        modelo.addAttribute("detalles", pagoPedidoService.detallesDe(pedido));
        modelo.addAttribute("seguimientoPrivado", true);
        return "paginas/pedidos/consultar";
    }

    @GetMapping("/pedidos/consultar")
    public String consultar(
            @RequestParam(
                    value = "codigo",
                    required = false
            )
            String codigo,
            Model model, HttpSession sesion
    ) {
        model.addAttribute(
                "titulo",
                "Detalle del pedido"
        );

        model.addAttribute(
                "codigoBusqueda",
                codigo
        );

        if (codigo == null || codigo.isBlank()) {
            return "paginas/pedidos/consultar";
        }

        return pagoPedidoService
                .buscarPorCodigo(codigo.trim())
                .map(pedido -> {

                    if (pedido.getCodigoPedido().equals(sesion.getAttribute("ultimoPedidoConfirmado"))
                            && pedido.getTokenSeguimiento() != null) {
                        model.addAttribute("tokenSeguimiento", pedido.getTokenSeguimiento());
                    }
                    inicializarPedido(pedido);
                    model.addAttribute("estadoCorreo",
                            colaCorreoService.estadoConfirmacion("PEDIDO", pedido.getIdPedido()));

                    model.addAttribute(
                            "pedido",
                            pedido
                    );

                    model.addAttribute(
                            "detalles",
                            pagoPedidoService.detallesDe(
                                    pedido
                            )
                    );

                    Pago pago =
                            pagoPedidoService
                                    .ultimoPagoDe(pedido)
                                    .orElse(null);

                    if (pago != null) {
                        model.addAttribute(
                                "pago",
                                pago
                        );
                    }

                    model.addAttribute(
                            "mensajeEstado",
                            mensajeConsulta(
                                    pedido,
                                    pago
                            )
                    );

                    model.addAttribute(
                            "mensajeTraslado",
                            mensajeEntrega(pedido)
                    );

                    return "paginas/pedidos/consultar";
                })
                .orElseGet(() -> {
                    model.addAttribute(
                            "error",
                            "No se encontró un pedido "
                            + "con ese código."
                    );

                    return "paginas/pedidos/consultar";
                });
    }

    private void cargarDatosCheckout(
            Model model,
            HttpSession session,
            Authentication authentication
    ) {
        model.addAttribute(
                "titulo",
                "Completar pedido"
        );

        model.addAttribute(
                "items",
                carritoService.obtenerItems(session)
        );

        model.addAttribute(
                "subtotal",
                carritoService.subtotal(session)
        );

        model.addAttribute(
                "costoDelivery",
                0
        );

        model.addAttribute(
                "mensajeTraslado",
                PedidoService.MENSAJE_TRASLADO
        );

        model.addAttribute(
                "mensajeRecojo",
                PedidoService.MENSAJE_RECOJO
        );

        model.addAttribute(
                "clienteLogueado",
                esClienteLogueado(authentication)
        );
    }

    private void prellenarSiCliente(
            PedidoFormDTO formulario,
            Authentication authentication
    ) {
        Usuario cliente =
                obtenerClienteLogueado(
                        authentication
                );

        if (cliente == null) {
            return;
        }

        formulario.setDni(
                cliente.getDni()
        );

        formulario.setNombres(
                cliente.getNombres()
        );

        formulario.setApellidos(
                cliente.getApellidos()
        );

        formulario.setCorreo(cliente.getCorreo());
        formulario.setTelefono(
                cliente.getTelefono()
        );
    }

    private String mensajePedidoRegistrado(
            Pedido pedido
    ) {
        String modalidad =
                pedido.getTipoPedido()
                        == TipoPedido.RECOJO_LOCAL
                        ? "Recojo en local"
                        : "Delivery";

        return "Pedido registrado correctamente. "
                + "Modalidad: "
                + modalidad
                + ". Tu código es "
                + pedido.getCodigoPedido()
                + ". Ahora realiza el pago mediante "
                + "Yape, Plin o transferencia bancaria "
                + "y adjunta la captura.";
    }

    private String mensajeEntrega(
            Pedido pedido
    ) {
        if (
                pedido.getTipoPedido()
                        == TipoPedido.RECOJO_LOCAL
        ) {
            return PedidoService.MENSAJE_RECOJO;
        }

        return PedidoService.MENSAJE_TRASLADO;
    }

    private String mensajeEstadoNoPago(
            Pedido pedido
    ) {
        return switch (pedido.getEstado()) {

            case PENDIENTE_VALIDACION ->
                    "Tu pago ya está en revisión. "
                    + "No necesitas volver a enviar "
                    + "la evidencia.";

            case CONFIRMADO ->
                    pedido.getTipoPedido()
                            == TipoPedido.RECOJO_LOCAL
                            ? "Tu pedido está confirmado "
                            + "para recojo en el local."
                            : "Tu pedido delivery está "
                            + "confirmado.";

            case CANCELADO ->
                    "Este pedido está cancelado "
                    + "y no admite pagos.";

            case FINALIZADO ->
                    "Este pedido está finalizado "
                    + "y no admite pagos.";

            default ->
                    "Este pedido no está pendiente "
                    + "de pago. Estado actual: "
                    + pedido.getEstado()
                    + ".";
        };
    }

    private String mensajeConsulta(
            Pedido pedido,
            Pago pago
    ) {
        return switch (pedido.getEstado()) {

            case PENDIENTE_PAGO -> {
                if (
                        pago != null
                                && pago.getEstado()
                                == Pago.EstadoPago.RECHAZADO
                ) {
                    String motivo =
                            pago.getObservacionValidacion()
                                    != null
                                    ? pago.getObservacionValidacion()
                                    : "Sin detalle";

                    yield "El pago fue rechazado. "
                            + "Motivo: "
                            + motivo
                            + ". Puedes enviar una "
                            + "nueva captura.";
                }

                yield "Tu pedido está pendiente "
                        + "de pago. Realiza el pago "
                        + "mediante Yape, Plin o "
                        + "transferencia bancaria.";
            }

            case PENDIENTE_VALIDACION ->
                    "Tu pago está en revisión. "
                    + "Caja validará la captura.";

            case CONFIRMADO ->
                    pedido.getTipoPedido()
                            == TipoPedido.RECOJO_LOCAL
                            ? "Tu pedido está confirmado "
                            + "para recojo en el restaurante."
                            : "Tu pedido delivery está "
                            + "confirmado. El restaurante irá actualizando su avance.";

            case CANCELADO ->
                    "El pedido fue cancelado.";

            case FINALIZADO ->
                    "El pedido fue finalizado.";
        };
    }

    private void inicializarPedido(
            Pedido pedido
    ) {
        if (pedido.getCliente() != null) {
            pedido.getCliente().getNombres();
        }

        if (pedido.getDireccion() != null) {
            pedido.getDireccion().getDireccion();
        }
    }

    private boolean esClienteLogueado(
            Authentication authentication
    ) {
        return obtenerClienteLogueado(
                authentication
        ) != null;
    }

    private Usuario obtenerClienteLogueado(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication
                        instanceof AnonymousAuthenticationToken
        ) {
            return null;
        }

        return usuarioRepository
                .findByCorreo(
                        authentication.getName()
                )
                .filter(usuario ->
                        usuario.getRol() != null
                                && "CLIENTE"
                                .equalsIgnoreCase(
                                        usuario.getRol()
                                                .getNombre()
                                )
                )
                .orElse(null);
    }
}