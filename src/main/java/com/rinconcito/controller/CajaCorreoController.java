package com.rinconcito.controller;

import com.rinconcito.service.NotaVentaService;
import com.rinconcito.service.correo.ColaCorreoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@ConditionalOnProperty(name="app.correo.habilitado",havingValue="true")
public class CajaCorreoController {
    private final ColaCorreoService cola;
    private final NotaVentaService notas;
    public CajaCorreoController(ColaCorreoService cola,NotaVentaService notas){this.cola=cola;this.notas=notas;}

    @GetMapping("/caja/correos")
    public String listar(Model model) {
        model.addAttribute("correos",cola.listar());
        return "paginas/caja/correos";
    }

    @PostMapping("/caja/correos/{id}/reintentar")
    public String reintentar(@PathVariable long id, RedirectAttributes flash) {
        try {cola.reintentar(id);flash.addFlashAttribute("mensaje","Se programo otro intento de envio.");}
        catch(IllegalStateException error){flash.addFlashAttribute("error",error.getMessage());}
        return "redirect:/caja/correos";
    }

    @PostMapping("/caja/notas-venta/{id}/correo")
    public String reenviar(@PathVariable Integer id,RedirectAttributes flash) {
        try {cola.nota(notas.obtenerParaPdf(id),true);flash.addFlashAttribute("mensaje","Solicitud registrada. Revisa el estado del correo.");}
        catch(IllegalStateException error){flash.addFlashAttribute("error",error.getMessage());}
        return "redirect:/caja/correos";
    }
}
