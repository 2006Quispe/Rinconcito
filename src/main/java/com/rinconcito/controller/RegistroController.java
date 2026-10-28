package com.rinconcito.controller;

import com.rinconcito.dto.RegistroFormDTO;
import com.rinconcito.entity.Usuario;
import com.rinconcito.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository
            securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public RegistroController(
            UsuarioService usuarioService,
            AuthenticationManager authenticationManager
    ) {
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
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
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response
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
            Usuario usuarioCreado =
                    usuarioService.registrar(
                            registroForm
                    );

            iniciarSesion(
                    usuarioCreado.getCorreo(),
                    registroForm.getContrasena(),
                    request,
                    response
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

    private void iniciarSesion(
            String correo,
            String contrasena,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UsernamePasswordAuthenticationToken
                solicitudAutenticacion =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                correo,
                                contrasena
                        );

        solicitudAutenticacion.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        Authentication autenticacion =
                authenticationManager.authenticate(
                        solicitudAutenticacion
                );

        SecurityContext contexto =
                SecurityContextHolder
                        .createEmptyContext();

        contexto.setAuthentication(
                autenticacion
        );

        SecurityContextHolder.setContext(
                contexto
        );

        securityContextRepository.saveContext(
                contexto,
                request,
                response
        );
    }
}
