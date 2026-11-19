package com.rinconcito.controller;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.service.ReservaService.MetodoDemo;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.ReservaService;
import com.rinconcito.service.correo.ColaCorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReservaController {

    private static final String PAGO_DEMO = "reservaPagoDemo";

    @Value("${app.reservas.pago-demo.habilitado:${APP_RESERVAS_PAGO_DEMO_HABILITADO:true}}")
    private boolean pagoDemoHabilitado;

    @Autowired
    private ColaCorreoService colaCorreoService;

    @Value("${app.correo.habilitado:false}")
    private boolean correoHabilitado;

    @ModelAttribute("correoHabilitado")
    public boolean correoHabilitado() {
        return correoHabilitado;
    }


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

    @GetMapping("/reservas/resumen")
    @ResponseBody
    public ResumenReserva resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaService.buscarEvento(fecha)
                .map(evento -> new ResumenReserva(evento.getTitulo(),
                        evento.isRequierePago() && evento.getPrecioEntrada() != null
                                ? Math.max(0, evento.getPrecioEntrada()) : 0))
                .orElseGet(() -> new ResumenReserva(null, 0));
    }

    public record ResumenReserva(String evento, int precioEntrada) {}

    @PostMapping("/reservas")
    public String procesarReserva(@ModelAttribute("reservaForm") ReservaFormDTO reservaForm,
                                  Model model,
                                  Authentication authentication, HttpSession sesion, RedirectAttributes redirect) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("pisos", pisoRepository.findByEstado(EstadoPiso.ACTIVO));
        model.addAttribute("clienteLogueado", esClienteLogueado(authentication));

        try {
            Usuario cliente = obtenerClienteLogueado(authentication);
            if (pagoDemoHabilitado && reservaForm.getFecha() != null) {
                ResumenReserva cotizacion = resumen(reservaForm.getFecha());
                if (cotizacion.precioEntrada() > 0) {
                    reservaService.validarCamposBasicos(reservaForm);
                    BorradorPago borrador = new BorradorPago(reservaForm, cliente, cotizacion);
                    sesion.setAttribute(PAGO_DEMO, borrador);
                    redirect.addAttribute("intento", borrador.intento);
                    return "redirect:/reservas/pago-demo";
                }
            }
            Reserva reserva = reservaService.crearReserva(reservaForm, cliente);
            model.addAttribute("exito", true);
            model.addAttribute("reserva", reserva);
            model.addAttribute("estadoCorreo",
                    colaCorreoService.estadoConfirmacion("RESERVA", reserva.getIdReserva()));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reservaForm", reservaForm);
        }

        return "paginas/reservas";
    }

    @GetMapping("/reservas/pago-demo")
    public String mostrarPagoDemo(@RequestParam String intento, HttpSession sesion,
                                  Authentication authentication, Model model) {
        model.addAttribute("titulo", "Pago de la reserva");
        BorradorPago borrador = obtenerBorrador(intento, sesion, authentication);
        if (borrador == null) {
            model.addAttribute("error", "La sesión de pago venció. Vuelve a completar tu reserva.");
            return "paginas/reservas/pago-demo";
        }
        synchronized (borrador) {
            model.addAttribute("intento", intento);
            model.addAttribute("evento", borrador.cotizacion.evento());
            model.addAttribute("precioEntrada", borrador.cotizacion.precioEntrada());
            model.addAttribute("personas", borrador.formulario.getNumeroPersonas());
            model.addAttribute("importe", borrador.importe());
            model.addAttribute("metodo", borrador.metodo);
            if (borrador.reserva != null) {
                model.addAttribute("reserva", borrador.reserva);
                model.addAttribute("estadoCorreo",
                        colaCorreoService.estadoConfirmacion("RESERVA", borrador.reserva.getIdReserva()));
            }
        }
        return "paginas/reservas/pago-demo";
    }

    @PostMapping("/reservas/pago-demo")
    public String procesarPagoDemo(@RequestParam String intento, @RequestParam MetodoDemo metodo,
                                    @RequestParam boolean aprobado, HttpSession sesion,
                                    Authentication authentication, RedirectAttributes redirect) {
        BorradorPago borrador = obtenerBorrador(intento, sesion, authentication);
        if (borrador == null) {
            redirect.addFlashAttribute("error", "La sesión de pago venció. Vuelve a completar tu reserva.");
            return "redirect:/reservas";
        }
        synchronized (borrador) {
            if (borrador.reserva == null) {
                borrador.metodo = metodo;
                if (aprobado) {
                    try {
                        borrador.reserva = reservaService.confirmarPagoDemo(
                                borrador.formulario, obtenerClienteLogueado(authentication),
                                borrador.importe(), metodo);
                    } catch (IllegalStateException e) {
                        redirect.addFlashAttribute("error", e.getMessage());
                    }
                } else {
                    redirect.addFlashAttribute("error", "Pago de prueba rechazado. Puedes volver a intentarlo.");
                }
            }
        }
        redirect.addAttribute("intento", intento);
        return "redirect:/reservas/pago-demo";
    }

    private BorradorPago obtenerBorrador(String intento, HttpSession sesion, Authentication authentication) {
        if (!pagoDemoHabilitado || !(sesion.getAttribute(PAGO_DEMO) instanceof BorradorPago borrador)
                || !borrador.intento.equals(intento) || Instant.now().isAfter(borrador.vence)) {
            return null;
        }
        Usuario cliente = obtenerClienteLogueado(authentication);
        Integer clienteId = cliente == null ? null : cliente.getIdUsuario();
        return Objects.equals(clienteId, borrador.clienteId) ? borrador : null;
    }

    private static final class BorradorPago {
        private final String intento = UUID.randomUUID().toString();
        private final Instant vence = Instant.now().plus(Duration.ofMinutes(15));
        private final ReservaFormDTO formulario;
        private final ResumenReserva cotizacion;
        private final Integer clienteId;
        private MetodoDemo metodo;
        private Reserva reserva;

        private BorradorPago(ReservaFormDTO formulario, Usuario cliente, ResumenReserva cotizacion) {
            this.formulario = formulario;
            this.cotizacion = cotizacion;
            this.clienteId = cliente == null ? null : cliente.getIdUsuario();
        }

        private int importe() {
            return cotizacion.precioEntrada() * formulario.getNumeroPersonas();
        }
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
