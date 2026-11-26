package com.rinconcito.controller;

import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.AdminPedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminPedidoController {

    private final AdminPedidoService adminPedidoService;

    private final UsuarioRepository usuarioRepository;

    public AdminPedidoController(
            AdminPedidoService adminPedidoService,
            UsuarioRepository usuarioRepository
    ) {
        this.adminPedidoService = adminPedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/pedidos/{idPedido}/cancelar")
    public String cancelarPedido(
            @PathVariable Integer idPedido,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            Usuario admin = usuarioRepository.findByCorreo(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("No encontramos tu cuenta administradora."));
            adminPedidoService.cancelarPedido(idPedido, admin, motivo);
            redirect.addFlashAttribute("exito", "Pedido cancelado. Los puntos asociados se revirtieron cuando correspondía.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/pedidos";
    }

    @GetMapping("/pedidos")
    public String listarPedidos(
            @RequestParam(
                    value = "fecha",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fecha,

            @RequestParam(
                    value = "estado",
                    required = false
            )
            EstadoPedido estado,

            @RequestParam(
                    value = "tipoPedido",
                    required = false
            )
            TipoPedido tipoPedido,

            @RequestParam(
                    value = "q",
                    required = false
            )
            String busqueda,

            Model model
    ) {
        var pedidos =
                adminPedidoService.listarConFiltros(
                        fecha,
                        estado,
                        tipoPedido,
                        busqueda
                );

        model.addAttribute(
                "titulo",
                "Administración de pedidos"
        );

        model.addAttribute(
                "pedidos",
                pedidos
        );

        model.addAttribute(
                "detallesPorPedido",
                adminPedidoService
                        .obtenerDetallesPorPedido(pedidos)
        );

        model.addAttribute(
                "estados",
                EstadoPedido.values()
        );

        model.addAttribute(
                "tiposPedido",
                TipoPedido.values()
        );

        model.addAttribute(
                "filtroFecha",
                fecha
        );

        model.addAttribute(
                "filtroEstado",
                estado
        );

        model.addAttribute(
                "filtroTipoPedido",
                tipoPedido
        );

        model.addAttribute(
                "filtroBusqueda",
                busqueda
        );

        model.addAttribute(
                "totalPedidos",
                pedidos.size()
        );

        model.addAttribute(
                "pendientesPago",
                adminPedidoService.contarPorEstado(
                        pedidos,
                        EstadoPedido.PENDIENTE_PAGO
                )
        );

        model.addAttribute(
                "pendientesValidacion",
                adminPedidoService.contarPorEstado(
                        pedidos,
                        EstadoPedido.PENDIENTE_VALIDACION
                )
        );

        model.addAttribute(
                "confirmados",
                adminPedidoService.contarPorEstado(
                        pedidos,
                        EstadoPedido.CONFIRMADO
                )
        );

        model.addAttribute(
                "totalImporte",
                adminPedidoService.sumarTotales(pedidos)
        );

        return "paginas/admin/pedidos";
    }
}
