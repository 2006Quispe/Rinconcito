package com.rinconcito.controller;

import com.rinconcito.dto.RegistroFormDTO;
import com.rinconcito.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistroController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {
        model.addAttribute("titulo", "Registro");
        model.addAttribute("registroForm", new RegistroFormDTO());
        return "paginas/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute("registroForm") RegistroFormDTO registroForm, Model model) {
        model.addAttribute("titulo", "Registro");

        try {
            usuarioService.registrar(registroForm);
            model.addAttribute("exito", true);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registroForm", registroForm);
        }

        return "paginas/registro";
    }
}