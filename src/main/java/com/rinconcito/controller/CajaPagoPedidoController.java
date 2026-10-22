package com.rinconcito.controller;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.EvidenciaStorageService;
import com.rinconcito.service.PagoPedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/caja")
public class CajaPagoPedidoController {

    private static final Logger log = LoggerFactory.getLogger(CajaPagoPedidoController.class);

    private final PagoPedidoService
            pagoPedidoService;

    private final EvidenciaStorageService
            evidenciaStorageService;

    private final UsuarioRepository
            usuarioRepository;

    public CajaPagoPedidoController(
            PagoPedidoService pagoPedidoService,
            EvidenciaStorageService evidenciaStorageService,
            UsuarioRepository usuarioRepository
    ) {
        this.pagoPedidoService =
                pagoPedidoService;

        this.evidenciaStorageService =
                evidenciaStorageService;

        this.usuarioRepository =
                usuarioRepository;
    }

    /*
     * Pagos online enviados mediante captura
     * que todavía deben ser validados por Caja.
     */
    @GetMapping("/pedidos-pendientes")
    public String listarPendientes(
            Model model
    ) {
        List<Pago> pendientes =
                pagoPedidoService
                        .listarPendientesValidacionPedidos();

        Map<Integer, List<DetallePedido>>
                detallesPorPago =
                new LinkedHashMap<>();

        for (Pago pago : pendientes) {

            Pedido pedido =
                    pago.getPedido();

            if (pedido == null) {
                continue;
            }

            pedido.getCodigoPedido();

            if (pedido.getCliente() != null) {
                pedido.getCliente().getNombres();
                pedido.getCliente().getApellidos();
                pedido.getCliente().getTelefono();
                pedido.getCliente().getDni();
            }

            if (pedido.getDireccion() != null) {
                pedido.getDireccion().getDireccion();
                pedido.getDireccion().getDistrito();
                pedido.getDireccion().getReferencia();
            }

            detallesPorPago.put(
                    pago.getIdPago(),
                    pagoPedidoService.detallesDe(
                            pedido
                    )
            );
        }

        model.addAttribute(
                "titulo",
                "Pedidos - Pagos por validar"
        );

        model.addAttribute(
                "pendientes",
                pendientes
        );

        model.addAttribute(
                "detallesPorPago",
                detallesPorPago
        );

        return "paginas/caja/pedidos-pendientes";
    }

    /*
     * Pedidos presenciales enviados por los
     * meseros para realizar el cobro en Caja.
     */
    @GetMapping("/pedidos-presenciales")
    public String listarPedidosPresenciales(
            Model model
    ) {
        List<Pedido> pedidos =
                pagoPedidoService
                        .listarPedidosPresencialesPendientesCobro();

        Map<Integer, List<DetallePedido>>
                detallesPorPedido =
                new LinkedHashMap<>();

        for (Pedido pedido : pedidos) {

            List<DetallePedido> detalles =
                    pagoPedidoService
                            .detallesDe(pedido);

            detallesPorPedido.put(
                    pedido.getIdPedido(),
                    detalles
            );
        }

        model.addAttribute(
                "titulo",
                "Cobros presenciales"
        );

        model.addAttribute(
                "pedidos",
                pedidos
        );

        model.addAttribute(
                "detallesPorPedido",
                detallesPorPedido
        );

        model.addAttribute(
                "metodosPago",
                MetodoPago.values()
        );

        return "paginas/caja/pedidos-presenciales";
    }

    /*
     * Registra el pago presencial, finaliza
     * el pedido y libera la mesa.
     */
    @PostMapping(
            "/pedidos-presenciales/{idPedido}/cobrar"
    )
    public String cobrarPedidoPresencial(
            @PathVariable Integer idPedido,

            @RequestParam("metodoPago")
            MetodoPago metodoPago,

            @RequestParam(
                    value = "numeroOperacion",
                    required = false
            )
            String numeroOperacion,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario validador =
                    usuarioActual(authentication);

            Pedido pedido =
                    pagoPedidoService
                            .registrarPagoPresencial(
                                    idPedido,
                                    metodoPago,
                                    numeroOperacion,
                                    validador
                            );

            // Con el ID podemos seguir el cobro sin guardar datos del cliente en el log.
            log.info("Cobro presencial registrado para el pedido {}", idPedido);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Pago del pedido "
                            + pedido.getCodigoPedido()
                            + " registrado correctamente. "
                            + "La nota de venta fue emitida "
                            + "y la mesa quedó disponible."
            );

        } catch (IllegalStateException e) {
            log.warn("No se pudo registrar el cobro presencial del pedido {}", idPedido);
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/caja/pedidos-presenciales";
    }

    @PostMapping(
            "/pedidos-pendientes/{idPago}/aprobar"
    )
    public String aprobar(
            @PathVariable Integer idPago,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario validador =
                    usuarioActual(authentication);

            pagoPedidoService.aprobar(
                    idPago,
                    validador
            );

            log.info("Pago de pedido {} aprobado", idPago);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Pago aprobado y pedido confirmado."
            );

        } catch (IllegalStateException e) {
            log.warn("No se pudo aprobar el pago de pedido {}", idPago);
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/caja/pedidos-pendientes";
    }

    @PostMapping(
            "/pedidos-pendientes/{idPago}/rechazar"
    )
    public String rechazar(
            @PathVariable Integer idPago,

            @RequestParam("motivo")
            String motivo,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario validador =
                    usuarioActual(authentication);

            pagoPedidoService.rechazar(
                    idPago,
                    validador,
                    motivo
            );

            log.info("Pago de pedido {} rechazado", idPago);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Pago rechazado. El pedido volvió "
                            + "a pendiente de pago."
            );

        } catch (IllegalStateException e) {
            log.warn("No se pudo rechazar el pago de pedido {}", idPago);
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/caja/pedidos-pendientes";
    }

    @GetMapping(
            "/evidencias-pedido/{idPago}"
    )
    public ResponseEntity<Resource>
    verEvidenciaPedido(
            @PathVariable Integer idPago
    ) {
        try {
            Pago pago =
                    pagoPedidoService
                            .obtenerPagoPedido(idPago);

            Path archivo =
                    evidenciaStorageService
                            .resolverArchivo(
                                    pago.getEvidenciaPago()
                            );

            Resource resource =
                    new UrlResource(
                            archivo.toUri()
                    );

            if (
                    !resource.exists()
                            || !resource.isReadable()
            ) {
                return ResponseEntity
                        .notFound()
                        .build();
            }

            String contentType =
                    evidenciaStorageService
                            .contentTypeDe(archivo);

            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + archivo.getFileName()
                                    + "\""
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .body(resource);

        } catch (IllegalStateException e) {
            log.debug("La evidencia del pago de pedido {} no esta disponible", idPago);
            return ResponseEntity
                    .notFound()
                    .build();

        } catch (Exception e) {
            log.error("No se pudo leer la evidencia del pago de pedido {}", idPago, e);
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    private Usuario usuarioActual(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || authentication.getName() == null
                        || authentication
                        .getName()
                        .isBlank()
        ) {
            throw new IllegalStateException(
                    "Debes iniciar sesión."
            );
        }

        return usuarioRepository
                .findByCorreo(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuario de sesión no encontrado."
                        )
                );
    }
}
