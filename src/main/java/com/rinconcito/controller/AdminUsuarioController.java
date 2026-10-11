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

            Model model
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

        return "paginas/admin/usuarios";
    }

    /*
     * Muestra el formulario para registrar un usuario.
     */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        prepararFormulario(
                model,
                new AdminUsuarioFormDTO(),
                "Registrar usuario"
        );

        return "paginas/admin/usuario-form";
    }

    /*
     * Muestra el formulario para editar un usuario.
     */
    @GetMapping("/{idUsuario}/editar")
    public String editar(
            @PathVariable Integer idUsuario,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            AdminUsuarioFormDTO formulario =
                    adminUsuarioService.obtenerFormulario(
                            idUsuario
                    );

            prepararFormulario(
                    model,
                    formulario,
                    "Editar usuario"
            );

            return "paginas/admin/usuario-form";

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

            prepararFormulario(
                    model,
                    formulario,
                    formulario.esNuevo()
                            ? "Registrar usuario"
                            : "Editar usuario"
            );

            return "paginas/admin/usuario-form";
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

    private void prepararFormulario(
            Model model,
            AdminUsuarioFormDTO formulario,
            String titulo
    ) {
        model.addAttribute(
                "titulo",
                titulo
        );

        model.addAttribute(
                "usuarioForm",
                formulario
        );

        model.addAttribute(
                "roles",
                adminUsuarioService.listarRoles()
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