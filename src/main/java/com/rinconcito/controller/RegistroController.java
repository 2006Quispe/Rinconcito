package com.rinconcito.controller;

import com.rinconcito.dto.RegistroFormDTO;
import com.rinconcito.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Registro"
        );

        if (!model.containsAttribute("registroForm")) {
            model.addAttribute(
                    "registroForm",
                    new RegistroFormDTO()
            );
        }

        return "paginas/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @Valid
            @ModelAttribute("registroForm")
            RegistroFormDTO registroForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        model.addAttribute(
                "titulo",
                "Registro"
        );

        if (
                !Objects.equals(
                        registroForm.getContrasena(),
                        registroForm.getConfirmarContrasena()
                )
        ) {
            bindingResult.rejectValue(
                    "confirmarContrasena",
                    "registro.contrasenas.no.coinciden",
                    "Las contraseñas no coinciden."
            );
        }

        if (bindingResult.hasErrors()) {
            return "paginas/registro";
        }

        try {
            usuarioService.registrar(
                    registroForm
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    true
            );

            return "redirect:/registro";

        } catch (IllegalStateException e) {
            bindingResult.reject(
                    "registro.no.procesado",
                    e.getMessage()
            );

            return "paginas/registro";
        }
    }
}
