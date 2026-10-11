package com.rinconcito.controller;

import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.PedidoRepository;
import com.rinconcito.repository.ReservaRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mi-cuenta")
public class MiCuentaController {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ReservaRepository reservaRepository;

    public MiCuentaController(
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            ReservaRepository reservaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.reservaRepository = reservaRepository;
    }

    @GetMapping({"", "/"})
    public String mostrarMiCuenta(
            Authentication authentication,
            Model model
    ) {
        Usuario cliente =
                obtenerClienteAutenticado(authentication);

        List<Pedido> todosLosPedidos =
                pedidoRepository
                        .findByClienteOrderByFechaRegistroDesc(
                                cliente
                        );

        List<Reserva> todasLasReservas =
                reservaRepository
                        .findByClienteOrderByFechaRegistroDesc(
                                cliente
                        );

        List<Pedido> pedidosActivos =
                todosLosPedidos.stream()
                        .filter(this::esPedidoActivo)
                        .toList();

        List<Pedido> historialPedidos =
                todosLosPedidos.stream()
                        .filter(pedido ->
                                !esPedidoActivo(pedido)
                        )
                        .toList();

        List<Reserva> reservasActivas =
                todasLasReservas.stream()
                        .filter(this::esReservaActiva)
                        .toList();

        List<Reserva> historialReservas =
                todasLasReservas.stream()
                        .filter(reserva ->
                                !esReservaActiva(reserva)
                        )
                        .toList();

        model.addAttribute(
                "titulo",
                "Mi cuenta"
        );

        model.addAttribute(
                "cliente",
                cliente
        );

        model.addAttribute(
                "pedidosActivos",
                pedidosActivos
        );

        model.addAttribute(
                "historialPedidos",
                historialPedidos
        );

        model.addAttribute(
                "reservasActivas",
                reservasActivas
        );

        model.addAttribute(
                "historialReservas",
                historialReservas
        );

        model.addAttribute(
                "cantidadPedidos",
                todosLosPedidos.size()
        );

        model.addAttribute(
                "cantidadReservas",
                todasLasReservas.size()
        );

        return "paginas/mi-cuenta";
    }

    private Usuario obtenerClienteAutenticado(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || authentication.getName() == null
                        || authentication.getName().isBlank()
        ) {
            throw new IllegalStateException(
                    "Debes iniciar sesión para acceder a tu cuenta."
            );
        }

        return usuarioRepository
                .findByCorreo(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el usuario de la sesión."
                        )
                );
    }

    private boolean esPedidoActivo(
            Pedido pedido
    ) {
        if (pedido == null || pedido.getEstado() == null) {
            return false;
        }

        return pedido.getEstado()
                != EstadoPedido.CANCELADO
                && pedido.getEstado()
                != EstadoPedido.FINALIZADO;
    }

    private boolean esReservaActiva(
            Reserva reserva
    ) {
        if (reserva == null || reserva.getEstado() == null) {
            return false;
        }

        return reserva.getEstado()
                != EstadoReserva.CANCELADA
                && reserva.getEstado()
                != EstadoReserva.FINALIZADA;
    }
}