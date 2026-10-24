package com.rinconcito.controller;

import com.rinconcito.dto.AdminUsuarioFormDTO;
import com.rinconcito.service.AdminUsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(
            AdminUsuarioService adminUsuarioService
    ) {
        this.adminUsuarioService = adminUsuarioService;
    }

    /*
     * Lista, busca y filtra los usuarios.
     */
    @GetMapping
    public String listar(
            @RequestParam(required = false)
            String buscar,

            @RequestParam(required = false)
            Integer idRol,

            @RequestParam(required = false)
            String estado,

            @RequestParam(required = false)
            String tipoRegistro,

            @RequestParam(required = false)
            String modal,

            @RequestParam(required = false)
            Integer editar,

            Model model
    ) {
        prepararListado(
                model,
                buscar,
                idRol,
                estado,
                tipoRegistro
        );

        if (!model.containsAttribute("usuarioForm")) {
            if (editar != null) {
                try {
                    model.addAttribute(
                            "usuarioForm",
                            adminUsuarioService
                                    .obtenerFormulario(editar)
                    );
                } catch (IllegalStateException e) {
                    model.addAttribute(
                            "error",
                            e.getMessage()
                    );
                    model.addAttribute(
                            "usuarioForm",
                            new AdminUsuarioFormDTO()
                    );
                    editar = null;
                }
            } else {
                model.addAttribute(
                        "usuarioForm",
                        new AdminUsuarioFormDTO()
                );
            }
        }

        model.addAttribute(
                "abrirModalUsuario",
                "usuario".equalsIgnoreCase(modal)
                        || editar != null
        );

        return "paginas/admin/usuarios";
    }

    /*
     * Muestra el formulario para registrar un usuario.
     */
    @GetMapping("/nuevo")
    public String nuevo() {
        return "redirect:/admin/usuarios?modal=usuario";
    }

    /*
     * Muestra el formulario para editar un usuario.
     */
    @GetMapping("/{idUsuario}/editar")
    public String editar(
            @PathVariable Integer idUsuario,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUsuarioService.obtenerFormulario(
                    idUsuario
            );

            return "redirect:/admin/usuarios?editar="
                    + idUsuario;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/usuarios";
        }
    }

    /*
     * Registra o actualiza un usuario.
     */
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("usuarioForm")
            AdminUsuarioFormDTO formulario,

            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String correoAdministrador =
                    obtenerCorreoSesion(
                            authentication
                    );

            boolean esNuevo =
                    formulario.esNuevo();

            adminUsuarioService.guardar(
                    formulario,
                    correoAdministrador
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    esNuevo
                            ? "Usuario registrado correctamente."
                            : "Usuario actualizado correctamente."
            );

            return "redirect:/admin/usuarios";

        } catch (IllegalStateException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "usuarioForm",
                    formulario
            );

            prepararListado(
                    model,
                    null,
                    null,
                    null,
                    null
            );

            model.addAttribute(
                    "abrirModalUsuario",
                    true
            );

            return "paginas/admin/usuarios";
        }
    }

    /*
     * Cambia automáticamente el estado:
     * ACTIVO -> INACTIVO
     * INACTIVO -> ACTIVO
     */
    @PostMapping("/{idUsuario}/estado")
    public String cambiarEstado(
            @PathVariable Integer idUsuario,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String correoAdministrador =
                    obtenerCorreoSesion(
                            authentication
                    );

            adminUsuarioService.cambiarEstado(
                    idUsuario,
                    correoAdministrador
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Estado del usuario actualizado correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/usuarios";
    }

    /*
     * Restablece la contraseña de un usuario.
     */
    @PostMapping("/{idUsuario}/contrasena")
    public String restablecerContrasena(
            @PathVariable Integer idUsuario,

            @RequestParam("nuevaContrasena")
            String nuevaContrasena,

            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUsuarioService.restablecerContrasena(
                    idUsuario,
                    nuevaContrasena
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Contraseña restablecida correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/usuarios";
    }

    private void prepararListado(
            Model model,
            String buscar,
            Integer idRol,
            String estado,
            String tipoRegistro
    ) {
        model.addAttribute(
                "titulo",
                "Personal y usuarios"
        );

        model.addAttribute(
                "usuarios",
                adminUsuarioService.buscar(
                        buscar,
                        idRol,
                        estado,
                        tipoRegistro
                )
        );

        model.addAttribute(
                "roles",
                adminUsuarioService.listarRoles()
        );

        model.addAttribute("buscar", buscar);
        model.addAttribute(
                "idRolSeleccionado",
                idRol
        );
        model.addAttribute(
                "estadoSeleccionado",
                estado
        );
        model.addAttribute(
                "tipoRegistroSeleccionado",
                tipoRegistro
        );
    }

    private String obtenerCorreoSesion(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || authentication.getName() == null
                        || authentication.getName().isBlank()
        ) {
            throw new IllegalStateException(
                    "No se pudo identificar al administrador."
            );
        }

        return authentication.getName();
    }
}