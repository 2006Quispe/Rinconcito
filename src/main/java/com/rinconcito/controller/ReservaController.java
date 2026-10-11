package com.rinconcito.controller;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PisoRepository pisoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/reservas")
    public String mostrarFormulario(Model model, Authentication authentication) {
        model.addAttribute("titulo", "Reservas");
        ReservaFormDTO form = new ReservaFormDTO();
        prellenarSiCliente(form, authentication);
        model.addAttribute("reservaForm", form);
        model.addAttribute("pisos", pisoRepository.findByEstado(EstadoPiso.ACTIVO));
        model.addAttribute("clienteLogueado", esClienteLogueado(authentication));
        return "paginas/reservas";
    }

    @PostMapping("/reservas")
    public String procesarReserva(@ModelAttribute("reservaForm") ReservaFormDTO reservaForm,
                                  Model model,
                                  Authentication authentication) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("pisos", pisoRepository.findByEstado(EstadoPiso.ACTIVO));
        model.addAttribute("clienteLogueado", esClienteLogueado(authentication));

        try {
            Usuario cliente = obtenerClienteLogueado(authentication);
            Reserva reserva = reservaService.crearReserva(reservaForm, cliente);
            model.addAttribute("exito", true);
            model.addAttribute("reserva", reserva);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reservaForm", reservaForm);
        }

        return "paginas/reservas";
    }

    private void prellenarSiCliente(ReservaFormDTO form, Authentication authentication) {
        Usuario cliente = obtenerClienteLogueado(authentication);
        if (cliente == null) {
            return;
        }
        form.setDni(cliente.getDni());
        form.setNombres(cliente.getNombres());
        form.setApellidos(cliente.getApellidos());
        form.setTelefono(cliente.getTelefono());
        if (cliente.getCorreo() != null && !cliente.getCorreo().endsWith("@invitado.rinconcito.pe")) {
            form.setCorreo(cliente.getCorreo());
        }
    }

    private boolean esClienteLogueado(Authentication authentication) {
        return obtenerClienteLogueado(authentication) != null;
    }

    private Usuario obtenerClienteLogueado(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .filter(u -> u.getRol() != null && "CLIENTE".equalsIgnoreCase(u.getRol().getNombre()))
                .orElse(null);
    }
}
