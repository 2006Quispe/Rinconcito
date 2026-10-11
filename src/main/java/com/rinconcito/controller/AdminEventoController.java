package com.rinconcito.controller;

import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import com.rinconcito.service.EventoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/eventos")
public class AdminEventoController {

    private final EventoService eventoService;

    public AdminEventoController(
            EventoService eventoService
    ) {
        this.eventoService =
                eventoService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Gestión de eventos"
        );

        model.addAttribute(
                "eventos",
                eventoService.listarTodos()
        );

        return "paginas/admin/eventos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {
        Evento evento =
                new Evento();

        evento.setEstado(
                EstadoEvento.ACTIVO
        );

        evento.setPrecioEntrada(0);

        model.addAttribute(
                "titulo",
                "Nuevo evento"
        );

        model.addAttribute(
                "evento",
                evento
        );

        model.addAttribute(
                "estados",
                EstadoEvento.values()
        );

        return "paginas/admin/eventos/formulario";
    }

    @GetMapping("/{idEvento}/editar")
    public String editar(
            @PathVariable Integer idEvento,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            Evento evento =
                    eventoService.buscarPorId(
                            idEvento
                    );

            model.addAttribute(
                    "titulo",
                    "Editar evento"
            );

            model.addAttribute(
                    "evento",
                    evento
            );

            model.addAttribute(
                    "estados",
                    EstadoEvento.values()
            );

            return "paginas/admin/eventos/formulario";

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/eventos";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Evento evento,
            RedirectAttributes ra
    ) {
        try {
            boolean nuevo =
                    evento.getIdEvento() == null;

            eventoService.guardar(evento);

            ra.addFlashAttribute(
                    "exito",
                    nuevo
                            ? "Evento registrado correctamente."
                            : "Evento actualizado correctamente."
            );

            return "redirect:/admin/eventos";

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            if (evento.getIdEvento() == null) {
                return "redirect:/admin/eventos/nuevo";
            }

            return "redirect:/admin/eventos/"
                    + evento.getIdEvento()
                    + "/editar";
        }
    }

    @PostMapping("/{idEvento}/estado")
    public String cambiarEstado(
            @PathVariable Integer idEvento,
            @RequestParam EstadoEvento estado,
            RedirectAttributes ra
    ) {
        try {
            eventoService.cambiarEstado(
                    idEvento,
                    estado
            );

            ra.addFlashAttribute(
                    "exito",
                    "Estado del evento actualizado."
            );

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/eventos";
    }
}