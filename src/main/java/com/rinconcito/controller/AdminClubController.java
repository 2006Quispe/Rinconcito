package com.rinconcito.controller;

import com.rinconcito.dto.ClubAjustePuntosFormDTO;
import com.rinconcito.dto.ClubCampanaFormDTO;
import com.rinconcito.dto.ClubConfiguracionFormDTO;
import com.rinconcito.dto.ClubRecompensaFormDTO;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import com.rinconcito.service.AdminClubService;
import com.rinconcito.service.ClubPuntosService;
import com.rinconcito.service.ProductoImagenStorageService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/club")
public class AdminClubController {

    private final AdminClubService adminClubService;
    private final ClubPuntosService clubPuntosService;
    private final ProductoImagenStorageService imagenStorage;
    private final UsuarioRepository usuarioRepository;

    public AdminClubController(
            AdminClubService adminClubService,
            ClubPuntosService clubPuntosService,
            ProductoImagenStorageService imagenStorage,
            UsuarioRepository usuarioRepository
    ) {
        this.adminClubService = adminClubService;
        this.clubPuntosService = clubPuntosService;
        this.imagenStorage = imagenStorage;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String index(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) Integer editarRecompensa,
            @RequestParam(required = false) Long editarCampana,
            Model model
    ) {
        ClubRecompensaFormDTO recompensa = editarRecompensa == null
                ? new ClubRecompensaFormDTO()
                : adminClubService.obtenerRecompensa(editarRecompensa);
        ClubCampanaFormDTO campana = editarCampana == null
                ? new ClubCampanaFormDTO()
                : adminClubService.obtenerCampana(editarCampana);
        poblar(model, q, recompensa, campana);
        model.addAttribute("filtroDni", dni == null ? "" : dni);
        if (dni != null && !dni.isBlank()) {
            var cliente = usuarioRepository.findByDni(dni.trim());
            if (cliente.isEmpty()) {
                model.addAttribute("errorDni", "No encontramos una cuenta de cliente con ese DNI.");
            } else {
                try {
                    int saldo = clubPuntosService.obtenerSaldo(cliente.get());
                    model.addAttribute("clienteClub", cliente.get());
                    model.addAttribute("saldoClienteClub", saldo);
                    model.addAttribute("movimientosClienteClub", clubPuntosService.listarMovimientos(cliente.get(), 0));
                } catch (IllegalStateException e) {
                    model.addAttribute("errorDni", e.getMessage());
                }
            }
        }
        return "paginas/admin/club";
    }

    @PostMapping("/recompensas/guardar")
    public String guardarRecompensa(
            @Valid @ModelAttribute("recompensaForm") ClubRecompensaFormDTO form,
            BindingResult errores,
            @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
            Authentication authentication,
            Model model,
            RedirectAttributes redirect
    ) {
        if (errores.hasErrors()) {
            poblar(model, "", form, new ClubCampanaFormDTO());
            model.addAttribute("erroresForm", "Revisa los campos requeridos y sus límites.");
            return "paginas/admin/club";
        }
        try {
            if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
                form.setImagen(imagenStorage.guardar(imagenArchivo));
            }
            adminClubService.guardarRecompensa(form, usuarioActual(authentication));
            redirect.addFlashAttribute("exitoClubAdmin", "Recompensa guardada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club";
    }

    @PostMapping("/configuracion")
    public String guardarConfiguracion(
            @Valid @ModelAttribute("configForm") ClubConfiguracionFormDTO form,
            BindingResult errores,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        if (errores.hasErrors()) {
            redirect.addFlashAttribute("errorClubAdmin", "Revisa los valores de acumulación y vigencia.");
            return "redirect:/admin/club#configuracion";
        }
        adminClubService.guardarConfiguracion(form, usuarioActual(authentication));
        redirect.addFlashAttribute("exitoClubAdmin", "Configuración del Club actualizada.");
        return "redirect:/admin/club#configuracion";
    }

    @PostMapping("/campanas/guardar")
    public String guardarCampana(
            @Valid @ModelAttribute("campanaForm") ClubCampanaFormDTO form,
            BindingResult errores,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        if (errores.hasErrors()) {
            redirect.addFlashAttribute("errorClubAdmin", "Completa la campaña con un porcentaje y fechas válidas.");
            return "redirect:/admin/club#campanas";
        }
        try {
            adminClubService.guardarCampana(form, usuarioActual(authentication));
            redirect.addFlashAttribute("exitoClubAdmin", "Campaña guardada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club#campanas";
    }

    @PostMapping("/ajustes")
    public String ajustarPuntos(
            @Valid @ModelAttribute("ajusteForm") ClubAjustePuntosFormDTO form,
            BindingResult errores,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        if (errores.hasErrors()) {
            redirect.addFlashAttribute("errorClubAdmin", "Indica un DNI válido, cantidad y motivo de al menos 8 caracteres.");
            return "redirect:/admin/club#ajustes";
        }
        try {
            Usuario cliente = usuarioRepository.findByDni(form.getDni().trim())
                    .orElseThrow(() -> new IllegalStateException("No encontramos ese cliente por DNI."));
            clubPuntosService.ajustarPuntos(cliente, usuarioActual(authentication),
                    form.getPuntos(), form.getMotivo());
            redirect.addFlashAttribute("exitoClubAdmin", "Ajuste registrado en el historial del cliente.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club#ajustes";
    }

    @PostMapping("/canjes/{idCanje}/cancelar")
    public String cancelarCanje(
            @PathVariable Long idCanje,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            clubPuntosService.cancelarCanje(idCanje, usuarioActual(authentication), motivo);
            redirect.addFlashAttribute("exitoClubAdmin", "Canje cancelado y puntos devueltos al cliente.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club#canjes";
    }

    @PostMapping("/canjes/validar")
    public String validarCanje(
            @RequestParam String codigo,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            String recompensa = clubPuntosService.validarCodigoEnCaja(codigo, usuarioActual(authentication));
            redirect.addFlashAttribute("exitoClubAdmin", "Canje validado: " + recompensa + ". Código " + codigo.trim().toUpperCase() + ".");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club#canjes";
    }

    @PostMapping("/pedidos/revertir")
    public String revertirPedido(
            @RequestParam Integer idPedido,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            clubPuntosService.revertirPorPedido(idPedido, usuarioActual(authentication), motivo);
            redirect.addFlashAttribute("exitoClubAdmin", "Revisión de puntos del pedido completada.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("errorClubAdmin", e.getMessage());
        }
        return "redirect:/admin/club#ajustes";
    }

    private void poblar(Model model, String q, ClubRecompensaFormDTO recompensa, ClubCampanaFormDTO campana) {
        model.addAttribute("titulo", "Administración del Club Rinconcito");
        model.addAttribute("recompensas", adminClubService.listarRecompensas());
        model.addAttribute("canjes", adminClubService.listarCanjes(q));
        model.addAttribute("productosActivos", adminClubService.listarProductosActivos());
        model.addAttribute("configForm", adminClubService.obtenerConfiguracion());
        model.addAttribute("campanas", adminClubService.listarCampanas());
        model.addAttribute("recompensaForm", recompensa);
        model.addAttribute("campanaForm", campana);
        model.addAttribute("ajusteForm", new ClubAjustePuntosFormDTO());
        model.addAttribute("filtroCanjes", q == null ? "" : q);
    }

    private Usuario usuarioActual(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Inicia sesión con una cuenta administradora.");
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("No encontramos la cuenta administradora."));
    }
}
