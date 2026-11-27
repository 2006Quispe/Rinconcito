package com.rinconcito.controller;

import com.rinconcito.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @GetMapping("/eventos")
    public String eventos(Model model) {
        model.addAttribute("titulo", "Eventos");
        model.addAttribute("proximos", eventoService.listarProximos());
        model.addAttribute("anteriores", eventoService.listarAnteriores());
        return "paginas/eventos";
    }
}
