package com.rinconcito.controller;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.MeseroHistorialService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mesero")
public class MeseroHistorialController {

    private final MeseroHistorialService
            meseroHistorialService;

    private final UsuarioRepository
            usuarioRepository;

    public MeseroHistorialController(
            MeseroHistorialService meseroHistorialService,
            UsuarioRepository usuarioRepository
    ) {
        this.meseroHistorialService =
                meseroHistorialService;

        this.usuarioRepository =
                usuarioRepository;
    }

    @GetMapping("/historial")
    public String mostrarHistorial(
            Authentication authentication,
            Model model
    ) {
        Usuario mesero =
                obtenerMesero(authentication);

        List<Pedido> pedidos =
                meseroHistorialService
                        .listarHistorial(mesero);

        Map<Integer, List<DetallePedido>>
                detallesPorPedido =
                new LinkedHashMap<>();

        for (Pedido pedido : pedidos) {
            detallesPorPedido.put(
                    pedido.getIdPedido(),
                    meseroHistorialService
                            .listarDetalles(pedido)
            );
        }

        model.addAttribute(
                "titulo",
                "Historial de atenciones"
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
                "totalFinalizados",
                meseroHistorialService
                        .contarFinalizados(pedidos)
        );

        model.addAttribute(
                "totalCancelados",
                meseroHistorialService
                        .contarCancelados(pedidos)
        );

        model.addAttribute(
                "totalVentas",
                meseroHistorialService
                        .sumarVentasFinalizadas(pedidos)
        );

        return "paginas/mesero/historial";
    }

    private Usuario obtenerMesero(
            Authentication authentication
    ) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "No se pudo identificar al usuario autenticado."
            );
        }

        return usuarioRepository
                .findByCorreo(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró al usuario mesero."
                        )
                );
    }
}