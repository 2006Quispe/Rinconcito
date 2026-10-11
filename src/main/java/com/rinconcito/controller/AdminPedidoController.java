package com.rinconcito.controller;

import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.service.AdminPedidoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminPedidoController {

    private final AdminPedidoService adminPedidoService;

    public AdminPedidoController(
            AdminPedidoService adminPedidoService
    ) {
        this.adminPedidoService = adminPedidoService;
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