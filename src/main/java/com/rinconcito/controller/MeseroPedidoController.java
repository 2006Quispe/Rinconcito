package com.rinconcito.controller;

import com.rinconcito.dto.ItemPedidoPresencialDTO;
import com.rinconcito.dto.PedidoPresencialFormDTO;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.PedidoPresencialService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mesero")
public class MeseroPedidoController {

    private final PedidoPresencialService
            pedidoPresencialService;

    private final UsuarioRepository
            usuarioRepository;

    public MeseroPedidoController(
            PedidoPresencialService pedidoPresencialService,
            UsuarioRepository usuarioRepository
    ) {
        this.pedidoPresencialService =
                pedidoPresencialService;

        this.usuarioRepository =
                usuarioRepository;
    }

    @GetMapping("/pedidos/nuevo")
    public String nuevoPedido(
            @RequestParam(
                    value = "idMesa",
                    required = false
            )
            Integer idMesa,

            @RequestParam(
                    value = "idReserva",
                    required = false
            )
            Integer idReserva,

            Model model
    ) {
        PedidoPresencialFormDTO formulario =
                new PedidoPresencialFormDTO();

        formulario.setIdMesa(idMesa);
        formulario.setIdReserva(idReserva);

        prepararFormularioNuevo(
                formulario,
                model
        );

        return "paginas/mesero/pedido-form";
    }

    @PostMapping("/pedidos/guardar")
    public String guardarPedido(
            @ModelAttribute("pedidoForm")
            PedidoPresencialFormDTO formulario,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario mesero =
                    obtenerMesero(
                            authentication
                    );

            Pedido pedido =
                    pedidoPresencialService
                            .registrarPedido(
                                    formulario,
                                    mesero
                            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Pedido presencial registrado correctamente."
            );

            return "redirect:/mesero/pedidos/"
                    + pedido.getIdPedido()
                    + "/comanda";

        } catch (IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            prepararFormularioNuevo(
                    formulario,
                    model
            );

            return "paginas/mesero/pedido-form";
        }
    }

    @GetMapping("/pedidos/{idPedido}/agregar")
    public String mostrarAgregarProductos(
            @PathVariable
            Integer idPedido,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Pedido pedido =
                    pedidoPresencialService
                            .obtenerPedido(
                                    idPedido
                            );

            PedidoPresencialFormDTO formulario =
                    new PedidoPresencialFormDTO();

            if (pedido.getMesa() != null) {
                formulario.setIdMesa(
                        pedido.getMesa()
                                .getIdMesa()
                );
            }

            prepararFormularioAgregar(
                    pedido,
                    formulario,
                    model
            );

            return "paginas/mesero/pedido-agregar-form";

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/mesero/pedidos";
        }
    }

    @PostMapping("/pedidos/{idPedido}/agregar")
    public String agregarProductos(
            @PathVariable
            Integer idPedido,

            @ModelAttribute("pedidoForm")
            PedidoPresencialFormDTO formulario,

            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario mesero =
                    obtenerMesero(
                            authentication
                    );

            Pedido pedido =
                    pedidoPresencialService
                            .agregarProductos(
                                    idPedido,
                                    formulario,
                                    mesero
                            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Productos agregados correctamente. "
                    + "Vuelve a imprimir la comanda "
                    + "para informar a cocina."
            );

            return "redirect:/mesero/pedidos/"
                    + pedido.getIdPedido()
                    + "/comanda";

        } catch (IllegalStateException e) {

            try {
                Pedido pedido =
                        pedidoPresencialService
                                .obtenerPedido(
                                        idPedido
                                );

                model.addAttribute(
                        "error",
                        e.getMessage()
                );

                prepararFormularioAgregar(
                        pedido,
                        formulario,
                        model
                );

                return "paginas/mesero/pedido-agregar-form";

            } catch (
                    IllegalStateException
                    pedidoNoEncontrado
            ) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        pedidoNoEncontrado.getMessage()
                );

                return "redirect:/mesero/pedidos";
            }
        }
    }

    @PostMapping(
            "/pedidos/{idPedido}/solicitar-cobro"
    )
    public String solicitarCobro(
            @PathVariable
            Integer idPedido,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario mesero =
                    obtenerMesero(
                            authentication
                    );

            Pedido pedido =
                    pedidoPresencialService
                            .solicitarCobro(
                                    idPedido,
                                    mesero
                            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "El pedido "
                    + pedido.getCodigoPedido()
                    + " fue enviado a Caja para cobrar."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/pedidos";
    }

    @GetMapping("/pedidos")
    public String listarPedidosActivos(
            Model model
    ) {
        List<Pedido> pedidos =
                pedidoPresencialService
                        .listarPedidosActivos();

        Map<Integer, List<DetallePedido>>
                detallesPorPedido =
                new LinkedHashMap<>();

        for (Pedido pedido : pedidos) {
            List<DetallePedido> detalles =
                    pedidoPresencialService
                            .listarDetalles(
                                    pedido
                            );

            detallesPorPedido.put(
                    pedido.getIdPedido(),
                    detalles
            );
        }

        model.addAttribute(
                "titulo",
                "Pedidos activos"
        );

        model.addAttribute(
                "pedidos",
                pedidos
        );

        model.addAttribute(
                "detallesPorPedido",
                detallesPorPedido
        );

        return "paginas/mesero/pedidos";
    }

    @GetMapping("/pedidos/{idPedido}/comanda")
    public String mostrarComanda(
            @PathVariable
            Integer idPedido,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Pedido pedido =
                    pedidoPresencialService
                            .obtenerPedido(
                                    idPedido
                            );

            List<DetallePedido> detalles =
                    pedidoPresencialService
                            .listarDetalles(
                                    pedido
                            );

            model.addAttribute(
                    "titulo",
                    "Comanda de cocina"
            );

            model.addAttribute(
                    "pedido",
                    pedido
            );

            model.addAttribute(
                    "detalles",
                    detalles
            );

            return "paginas/mesero/comanda";

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/mesero/pedidos";
        }
    }

    private void prepararFormularioNuevo(
            PedidoPresencialFormDTO formulario,
            Model model
    ) {
        List<Producto> productos =
                pedidoPresencialService
                        .listarProductosDisponibles();

        prepararItems(
                formulario,
                productos
        );

        model.addAttribute(
                "titulo",
                "Nuevo pedido presencial"
        );

        model.addAttribute(
                "pedidoForm",
                formulario
        );

        model.addAttribute(
                "mesas",
                pedidoPresencialService
                        .listarMesasOcupadas()
        );

        model.addAttribute(
                "productos",
                productos
        );
    }

    private void prepararFormularioAgregar(
            Pedido pedido,
            PedidoPresencialFormDTO formulario,
            Model model
    ) {
        List<Producto> productos =
                pedidoPresencialService
                        .listarProductosDisponibles();

        prepararItems(
                formulario,
                productos
        );

        model.addAttribute(
                "titulo",
                "Agregar productos"
        );

        model.addAttribute(
                "pedidoForm",
                formulario
        );

        model.addAttribute(
                "pedido",
                pedido
        );

        model.addAttribute(
                "productos",
                productos
        );
    }

    private void prepararItems(
            PedidoPresencialFormDTO formulario,
            List<Producto> productos
    ) {
        if (
                formulario.getItems() != null
                        && !formulario.getItems()
                                .isEmpty()
        ) {
            return;
        }

        List<ItemPedidoPresencialDTO> items =
                new ArrayList<>();

        for (Producto producto : productos) {
            ItemPedidoPresencialDTO item =
                    new ItemPedidoPresencialDTO();

            item.setIdProducto(
                    producto.getIdProducto()
            );

            item.setCantidad(0);
            item.setObservaciones(null);

            items.add(item);
        }

        formulario.setItems(items);
    }

    private Usuario obtenerMesero(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || authentication.getName() == null
                        || authentication.getName().isBlank()
        ) {
            throw new IllegalStateException(
                    "No se pudo identificar "
                    + "al usuario autenticado."
            );
        }

        return usuarioRepository
                .findByCorreo(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró "
                                + "al usuario mesero."
                        )
                );
    }
}