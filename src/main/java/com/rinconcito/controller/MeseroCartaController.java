package com.rinconcito.controller;

import com.rinconcito.service.ProductoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mesero")
public class MeseroCartaController {

    private final ProductoService productoService;

    public MeseroCartaController(
            ProductoService productoService
    ) {
        this.productoService = productoService;
    }

    @GetMapping("/carta")
    public String mostrarDisponibilidad(Model model) {
        model.addAttribute("titulo", "Disponibilidad de carta");
        model.addAttribute("productos", productoService.listarActivos());
        return "paginas/mesero/carta-disponibilidad";
    }

    @PostMapping("/carta/{idProducto}/disponibilidad")
    public String cambiarDisponibilidad(
            @PathVariable Integer idProducto,
            @RequestParam boolean disponible,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (authentication == null
                    || authentication.getName() == null
                    || authentication.getName().isBlank()) {
                throw new IllegalStateException(
                        "No se pudo identificar al mesero."
                );
            }

            productoService.cambiarDisponibilidad(
                    idProducto,
                    disponible,
                    authentication.getName()
            );
            redirectAttributes.addFlashAttribute(
                    "exito",
                    disponible
                            ? "Producto marcado como disponible."
                            : "Producto marcado como agotado."
            );
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mesero/carta";
    }
}
