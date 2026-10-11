package com.rinconcito.controller;

import com.rinconcito.service.GaleriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GaleriaController {

    @Autowired
    private GaleriaService galeriaService;

    @GetMapping("/galeria")
    public String galeria(Model model) {
        model.addAttribute("titulo", "Galería");
        model.addAttribute("imagenes", galeriaService.listarActivas());
        return "paginas/galeria";
    }
}
