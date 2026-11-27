package com.rinconcito.controller;

import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.ClubPuntosService;
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
@RequestMapping("/club")
public class ClubController {

    private final ClubPuntosService clubPuntosService;
    private final UsuarioRepository usuarioRepository;

    public ClubController(
            ClubPuntosService clubPuntosService,
            UsuarioRepository usuarioRepository
    ) {
        this.clubPuntosService = clubPuntosService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String mostrarClub(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) String categoria,
            Model model
    ) {
        Usuario cliente = clienteActual(authentication);
        int saldo = clubPuntosService.obtenerSaldo(cliente);
        int totalMovimientos = clubPuntosService.contarMovimientos(cliente);
        int paginasMovimientos = (totalMovimientos + 19) / 20;
        int paginaActual = Math.max(0, Math.min(pagina, Math.max(0, paginasMovimientos - 1)));
        var recompensas = clubPuntosService.listarRecompensas(saldo);
        var recompensasFiltradas = categoria == null || categoria.isBlank()
                || "TODAS".equals(categoria)
                ? recompensas
                : recompensas.stream()
                    .filter(r -> categoria.equals(r.categoria()))
                    .toList();
        model.addAttribute("titulo", "Club Rinconcito");
        model.addAttribute("cliente", cliente);
        model.addAttribute("saldo", saldo);
        model.addAttribute("nivelFidelizacion", clubPuntosService.obtenerNivelFidelizacion(cliente));
        model.addAttribute("puntosPorSol", clubPuntosService.obtenerConfiguracion("PUNTOS_POR_SOL", 1));
        model.addAttribute("recompensas", recompensasFiltradas);
        model.addAttribute("progreso", clubPuntosService.calcularProgreso(saldo, recompensas));
        model.addAttribute("categoriaActual", categoria == null ? "TODAS" : categoria);
        model.addAttribute("cupones", clubPuntosService.listarCanjes(cliente));
        model.addAttribute("movimientos", clubPuntosService.listarMovimientos(cliente, paginaActual));
        model.addAttribute("paginaMovimientos", paginaActual);
        model.addAttribute("hayMasMovimientos", paginaActual + 1 < paginasMovimientos);
        return "paginas/club";
    }

    @PostMapping("/canjear/{idRecompensa}")
    public String canjear(
            @PathVariable Integer idRecompensa,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario cliente = clienteActual(authentication);
            String codigo = clubPuntosService.canjear(cliente, idRecompensa);
            redirectAttributes.addFlashAttribute(
                    "clubExito",
                    "¡Canje realizado! Guarda tu código " + codigo
                            + ". Usa el código de producto en tu pedido online o preséntalo en Caja; "
                            + "las experiencias familiares se validan presencialmente."
            );
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("clubError", e.getMessage());
        }
        return "redirect:/club#recompensas";
    }

    private Usuario clienteActual(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Inicia sesión para entrar al club.");
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "No encontramos la cuenta del cliente."
                ));
    }
}
