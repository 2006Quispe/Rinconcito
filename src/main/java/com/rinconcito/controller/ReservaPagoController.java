package com.rinconcito.controller;

import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Reserva;
import com.rinconcito.service.ConfiguracionRestauranteService;
import com.rinconcito.service.PagoReservaService;
import com.rinconcito.service.correo.ColaCorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReservaPagoController {

    @Autowired
    private ColaCorreoService colaCorreoService;

    @Value("${app.correo.habilitado:false}")
    private boolean correoHabilitado;

    @ModelAttribute("correoHabilitado")
    public boolean correoHabilitado() {
        return correoHabilitado;
    }


    @Autowired
    private PagoReservaService pagoReservaService;

    @Autowired
    private ConfiguracionRestauranteService configuracionRestauranteService;

    @GetMapping("/reservas/pago")
    public String mostrarPago(@RequestParam(value = "codigo", required = false) String codigo,
                              Model model) {
        model.addAttribute("titulo", "Pagar reserva");
        if (codigo == null || codigo.isBlank()) {
            model.addAttribute("error", "Indica el código de tu reserva.");
            return "paginas/reservas/pago";
        }

        return pagoReservaService.buscarPorCodigo(codigo.trim())
                .map(reserva -> {
                    model.addAttribute("reserva", reserva);
                    model.addAttribute("estadoCorreo",
                            colaCorreoService.estadoConfirmacion("RESERVA", reserva.getIdReserva()));
                    if (reserva.getEvento() != null) {
                        reserva.getEvento().getTitulo();
                    }
                    pagoReservaService.ultimoPagoDe(reserva).ifPresent(p -> model.addAttribute("pago", p));
                    if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
                        model.addAttribute("info", mensajeEstadoNoPago(reserva));
                        return "paginas/reservas/pago";
                    }
                    ConfiguracionRestaurante cfg = configuracionRestauranteService.obtener();
                    model.addAttribute("configPago", cfg);
                    return "paginas/reservas/pago";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "No se encontró una reserva con el código " + codigo.trim() + ".");
                    return "paginas/reservas/pago";
                });
    }

    @PostMapping("/reservas/pago")
    public String enviarEvidencia(@RequestParam("codigo") String codigo,
                                  @RequestParam("metodoPago") String metodoPago,
                                  @RequestParam(value = "numeroOperacion", required = false) String numeroOperacion,
                                  @RequestParam("captura") MultipartFile captura,
                                  RedirectAttributes ra) {
        try {
            MetodoPago metodo = MetodoPago.valueOf(metodoPago.trim().toUpperCase());
            pagoReservaService.registrarEvidencia(codigo, metodo, numeroOperacion, captura);
            ra.addFlashAttribute("exito",
                    "Evidencia enviada. Tu pago está en revisión. Guarda tu código para consultar el estado.");
            return "redirect:/reservas/consultar?codigo=" + codigo.trim();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Método de pago inválido. Elige Yape o Plin.");
            return "redirect:/reservas/pago?codigo=" + (codigo != null ? codigo.trim() : "");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/pago?codigo=" + (codigo != null ? codigo.trim() : "");
        }
    }

    @GetMapping("/reservas/consultar")
    public String consultar(@RequestParam(value = "codigo", required = false) String codigo,
                            Model model) {
        model.addAttribute("titulo", "Consultar reserva");
        model.addAttribute("codigoBusqueda", codigo);
        if (codigo == null || codigo.isBlank()) {
            return "paginas/reservas/consultar";
        }

        return pagoReservaService.buscarPorCodigo(codigo.trim())
                .map(reserva -> {
                    if (reserva.getEvento() != null) {
                        reserva.getEvento().getTitulo();
                    }
                    if (reserva.getPisoSolicitado() != null) {
                        reserva.getPisoSolicitado().getNombre();
                    }
                    if (reserva.getMesa() != null) {
                        reserva.getMesa().getNumero();
                    }
                    model.addAttribute("reserva", reserva);
                    model.addAttribute("estadoCorreo",
                            colaCorreoService.estadoConfirmacion("RESERVA", reserva.getIdReserva()));
                    Pago pago = pagoReservaService.ultimoPagoDe(reserva).orElse(null);
                    if (pago != null) {
                        model.addAttribute("pago", pago);
                    }
                    model.addAttribute("mensajeEstado", mensajeConsulta(reserva, pago));
                    return "paginas/reservas/consultar";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "No se encontró una reserva con ese código.");
                    return "paginas/reservas/consultar";
                });
    }

    private String mensajeEstadoNoPago(Reserva reserva) {
        return switch (reserva.getEstado()) {
            case PENDIENTE_VALIDACION -> "Tu pago ya está en revisión. No es necesario volver a enviar evidencia.";
            case CONFIRMADA -> "Tu reserva ya está confirmada. No requiere otro pago.";
            case CANCELADA, RECHAZADA -> "Esta reserva está " + reserva.getEstado() + " y no admite pago.";
            default -> "Esta reserva no está pendiente de pago (estado: " + reserva.getEstado() + ").";
        };
    }

    private String mensajeConsulta(Reserva reserva, Pago pago) {
        return switch (reserva.getEstado()) {
            case PENDIENTE_PAGO -> {
                if (pago != null && pago.getEstado() == Pago.EstadoPago.RECHAZADO) {
                    yield "Pago rechazado. Motivo: "
                            + (pago.getObservacionValidacion() != null
                            ? pago.getObservacionValidacion()
                            : "sin detalle")
                            + ". Puedes volver a enviar una captura.";
                }
                yield "Tu reserva está pendiente de pago. Envía la captura de Yape o Plin.";
            }
            case PENDIENTE_VALIDACION -> "Pago en revisión. Caja validará tu captura pronto.";
            case CONFIRMADA -> "Reserva confirmada. ¡Te esperamos!";
            case CANCELADA -> "Reserva cancelada.";
            case RECHAZADA -> "Reserva rechazada.";
            default -> "Estado: " + reserva.getEstado();
        };
    }
}
