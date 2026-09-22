package com.rinconcito.controller;

import com.rinconcito.entity.Evento;
import com.rinconcito.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @GetMapping("/eventos")
    public String eventos(Model model) {
        List<Evento> eventos = eventoService.listarActivos();
        model.addAttribute("titulo", "Eventos");
        model.addAttribute("eventos", eventos);
        return "paginas/eventos";
    }
}