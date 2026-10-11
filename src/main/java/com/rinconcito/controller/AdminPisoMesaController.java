package com.rinconcito.controller;

import com.rinconcito.dto.AdminMesaFormDTO;
import com.rinconcito.dto.AdminPisoFormDTO;
import com.rinconcito.entity.Piso;
import com.rinconcito.service.AdminPisoMesaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminPisoMesaController {

    private final AdminPisoMesaService adminPisoMesaService;

    public AdminPisoMesaController(
            AdminPisoMesaService adminPisoMesaService
    ) {
        this.adminPisoMesaService =
                adminPisoMesaService;
    }

    @GetMapping("/pisos-mesas")
    public String listar(
            @RequestParam(
                    name = "piso",
                    required = false
            ) Integer idPiso,
            Model model
    ) {
        List<Piso> pisos =
                adminPisoMesaService.listarPisos();

        Map<Integer, Long> totalMesasPorPiso =
                new HashMap<>();

        Map<Integer, Long> mesasDisponiblesPorPiso =
                new HashMap<>();

        Map<Integer, Long> mesasOcupadasPorPiso =
                new HashMap<>();

        Map<Integer, Long> mesasReservadasPorPiso =
                new HashMap<>();

        for (Piso piso : pisos) {
            totalMesasPorPiso.put(
                    piso.getIdPiso(),
                    adminPisoMesaService
                            .contarMesasPorPiso(piso)
            );

            mesasDisponiblesPorPiso.put(
                    piso.getIdPiso(),
                    adminPisoMesaService
                            .contarMesasDisponibles(piso)
            );

            mesasOcupadasPorPiso.put(
                    piso.getIdPiso(),
                    adminPisoMesaService
                            .contarMesasOcupadas(piso)
            );

            mesasReservadasPorPiso.put(
                    piso.getIdPiso(),
                    adminPisoMesaService
                            .contarMesasReservadas(piso)
            );
        }

        model.addAttribute(
                "titulo",
                "Pisos y mesas"
        );

        model.addAttribute(
                "pisos",
                pisos
        );

        model.addAttribute(
                "mesas",
                adminPisoMesaService
                        .listarMesas(idPiso)
        );

        model.addAttribute(
                "idPisoSeleccionado",
                idPiso
        );

        model.addAttribute(
                "totalMesasPorPiso",
                totalMesasPorPiso
        );

        model.addAttribute(
                "mesasDisponiblesPorPiso",
                mesasDisponiblesPorPiso
        );

        model.addAttribute(
                "mesasOcupadasPorPiso",
                mesasOcupadasPorPiso
        );

        model.addAttribute(
                "mesasReservadasPorPiso",
                mesasReservadasPorPiso
        );

        return "paginas/admin/pisos-mesas";
    }

    @GetMapping("/pisos/nuevo")
    public String nuevoPiso(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Nuevo piso"
        );

        model.addAttribute(
                "pisoForm",
                adminPisoMesaService
                        .nuevoFormularioPiso()
        );

        return "paginas/admin/piso-form";
    }

    @GetMapping("/pisos/{idPiso}/editar")
    public String editarPiso(
            @PathVariable Integer idPiso,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute(
                    "titulo",
                    "Editar piso"
            );

            model.addAttribute(
                    "pisoForm",
                    adminPisoMesaService
                            .obtenerFormularioPiso(idPiso)
            );

            return "paginas/admin/piso-form";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/pisos-mesas";
        }
    }

    @PostMapping("/pisos/guardar")
    public String guardarPiso(
            @ModelAttribute("pisoForm")
            AdminPisoFormDTO formulario,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPisoMesaService.guardarPiso(
                    formulario
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    formulario.esNuevo()
                            ? "Piso registrado correctamente."
                            : "Piso actualizado correctamente."
            );

            return "redirect:/admin/pisos-mesas";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            if (formulario.getIdPiso() == null) {
                return "redirect:/admin/pisos/nuevo";
            }

            return "redirect:/admin/pisos/"
                    + formulario.getIdPiso()
                    + "/editar";
        }
    }

    @PostMapping("/pisos/{idPiso}/estado")
    public String cambiarEstadoPiso(
            @PathVariable Integer idPiso,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Piso piso =
                    adminPisoMesaService
                            .cambiarEstadoPiso(idPiso);

            String mensaje =
                    piso.getEstado().name()
                            .equals("ACTIVO")
                            ? "Piso activado correctamente."
                            : "Piso desactivado correctamente.";

            redirectAttributes.addFlashAttribute(
                    "exito",
                    mensaje
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/pisos-mesas";
    }

    @GetMapping("/mesas/nueva")
    public String nuevaMesa(
            @RequestParam(
                    name = "piso",
                    required = false
            ) Integer idPiso,
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Nueva mesa"
        );

        model.addAttribute(
                "mesaForm",
                adminPisoMesaService
                        .nuevoFormularioMesa(idPiso)
        );

        model.addAttribute(
                "pisos",
                adminPisoMesaService
                        .listarPisosActivos()
        );

        return "paginas/admin/mesa-form";
    }

    @GetMapping("/mesas/{idMesa}/editar")
    public String editarMesa(
            @PathVariable Integer idMesa,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute(
                    "titulo",
                    "Editar mesa"
            );

            model.addAttribute(
                    "mesaForm",
                    adminPisoMesaService
                            .obtenerFormularioMesa(idMesa)
            );

            model.addAttribute(
                    "pisos",
                    adminPisoMesaService
                            .listarPisosActivos()
            );

            return "paginas/admin/mesa-form";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/pisos-mesas";
        }
    }

    @PostMapping("/mesas/guardar")
    public String guardarMesa(
            @ModelAttribute("mesaForm")
            AdminMesaFormDTO formulario,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPisoMesaService.guardarMesa(
                    formulario
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    formulario.esNueva()
                            ? "Mesa registrada correctamente."
                            : "Mesa actualizada correctamente."
            );

            String destino =
                    "redirect:/admin/pisos-mesas";

            if (formulario.getIdPiso() != null) {
                destino += "?piso="
                        + formulario.getIdPiso();
            }

            return destino;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            if (formulario.getIdMesa() == null) {
                String destino =
                        "redirect:/admin/mesas/nueva";

                if (formulario.getIdPiso() != null) {
                    destino += "?piso="
                            + formulario.getIdPiso();
                }

                return destino;
            }

            return "redirect:/admin/mesas/"
                    + formulario.getIdMesa()
                    + "/editar";
        }
    }

    @PostMapping("/mesas/{idMesa}/estado")
    public String cambiarEstadoMesa(
            @PathVariable Integer idMesa,
            @RequestParam(
                    name = "piso",
                    required = false
            ) Integer idPiso,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPisoMesaService
                    .cambiarEstadoMesa(idMesa);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Estado de la mesa actualizado correctamente."
            );

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        String destino =
                "redirect:/admin/pisos-mesas";

        if (idPiso != null) {
            destino += "?piso=" + idPiso;
        }

        return destino;
    }
}