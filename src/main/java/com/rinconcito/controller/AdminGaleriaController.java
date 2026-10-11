package com.rinconcito.controller;

import com.rinconcito.entity.Galeria;
import com.rinconcito.entity.Galeria.EstadoGaleria;
import com.rinconcito.service.GaleriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/galeria")
public class AdminGaleriaController {

    private final GaleriaService galeriaService;

    public AdminGaleriaController(
            GaleriaService galeriaService
    ) {
        this.galeriaService = galeriaService;
    }

    @GetMapping
    public String listar(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Administración de galería"
        );

        model.addAttribute(
                "imagenes",
                galeriaService.listarTodasAdmin()
        );

        return "paginas/admin/galeria";
    }

    @GetMapping("/nueva")
    public String nueva(
            Model model
    ) {
        prepararFormulario(
                model,
                new Galeria(),
                "Nueva imagen"
        );

        return "paginas/admin/galeria-form";
    }

    @GetMapping("/{idImagen}/editar")
    public String editar(
            @PathVariable Integer idImagen,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return galeriaService
                .buscarPorId(idImagen)
                .map(galeria -> {
                    prepararFormulario(
                            model,
                            galeria,
                            "Editar imagen"
                    );

                    return "paginas/admin/galeria-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "error",
                            "La imagen de la galería no existe."
                    );

                    return "redirect:/admin/galeria";
                });
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(
                    value = "idImagen",
                    required = false
            )
            Integer idImagen,

            @RequestParam(
                    value = "titulo",
                    required = false
            )
            String titulo,

            @RequestParam(
                    value = "descripcion",
                    required = false
            )
            String descripcion,

            @RequestParam(
                    value = "estado",
                    defaultValue = "ACTIVO"
            )
            EstadoGaleria estado,

            @RequestParam(
                    value = "archivoImagen",
                    required = false
            )
            MultipartFile archivoImagen,

            RedirectAttributes redirectAttributes
    ) {
        try {
            galeriaService.guardar(
                    idImagen,
                    titulo,
                    descripcion,
                    estado,
                    archivoImagen
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    idImagen == null
                            ? "Imagen publicada correctamente."
                            : "Publicación actualizada correctamente."
            );

            return "redirect:/admin/galeria";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            if (idImagen != null) {
                return "redirect:/admin/galeria/"
                        + idImagen
                        + "/editar";
            }

            return "redirect:/admin/galeria/nueva";
        }
    }

    @PostMapping("/{idImagen}/estado")
    public String cambiarEstado(
            @PathVariable Integer idImagen,

            @RequestParam("estado")
            EstadoGaleria estado,

            RedirectAttributes redirectAttributes
    ) {
        try {
            galeriaService.cambiarEstado(
                    idImagen,
                    estado
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    estado == EstadoGaleria.ACTIVO
                            ? "La imagen ahora está visible."
                            : "La imagen fue ocultada."
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/galeria";
    }

    @PostMapping("/{idImagen}/eliminar")
    public String eliminar(
            @PathVariable Integer idImagen,
            RedirectAttributes redirectAttributes
    ) {
        try {
            galeriaService.eliminar(idImagen);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La imagen fue eliminada definitivamente."
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/galeria";
    }

    private void prepararFormulario(
            Model model,
            Galeria galeria,
            String titulo
    ) {
        model.addAttribute(
                "titulo",
                titulo
        );

        model.addAttribute(
                "galeria",
                galeria
        );

        model.addAttribute(
                "estados",
                EstadoGaleria.values()
        );
    }
}