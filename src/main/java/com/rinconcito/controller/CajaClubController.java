package com.rinconcito.controller;

import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.ClubPuntosService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caja/club")
public class CajaClubController {

    private final ClubPuntosService clubPuntosService;
    private final UsuarioRepository usuarioRepository;

    public CajaClubController(ClubPuntosService clubPuntosService, UsuarioRepository usuarioRepository) {
        this.clubPuntosService = clubPuntosService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrar(Model model) {
        model.addAttribute("titulo", "Validación Club Rinconcito");
        return "paginas/caja/club-validar";
    }

    @PostMapping("/validar")
    public String validar(
            @RequestParam String codigo,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            Usuario cajero = usuarioRepository.findByCorreo(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("No encontramos tu usuario de Caja."));
            String recompensa = clubPuntosService.validarCodigoEnCaja(codigo, cajero);
            redirect.addFlashAttribute("canjeValidado", recompensa);
            redirect.addFlashAttribute("codigoValidado", codigo.trim().toUpperCase());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorValidacion", e.getMessage());
        }
        return "redirect:/caja/club";
    }
}
