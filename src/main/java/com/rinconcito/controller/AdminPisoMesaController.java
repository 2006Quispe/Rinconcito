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

            @RequestParam(
                    name = "modal",
                    required = false
            ) String modal,

            @RequestParam(
                    name = "editarPiso",
                    required = false
            ) Integer idPisoEditar,

            @RequestParam(
                    name = "editarMesa",
                    required = false
            ) Integer idMesaEditar,

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

        model.addAttribute(
                "pisosActivos",
                adminPisoMesaService.listarPisosActivos()
        );

        if (!model.containsAttribute("pisoForm")) {
            if (idPisoEditar != null) {
                try {
                    model.addAttribute(
                            "pisoForm",
                            adminPisoMesaService
                                    .obtenerFormularioPiso(
                                            idPisoEditar
                                    )
                    );
                } catch (IllegalStateException e) {
                    model.addAttribute(
                            "error",
                            e.getMessage()
                    );
                    model.addAttribute(
                            "pisoForm",
                            adminPisoMesaService
                                    .nuevoFormularioPiso()
                    );
                    idPisoEditar = null;
                }
            } else {
                model.addAttribute(
                        "pisoForm",
                        adminPisoMesaService
                                .nuevoFormularioPiso()
                );
            }
        }

        if (!model.containsAttribute("mesaForm")) {
            if (idMesaEditar != null) {
                try {
                    model.addAttribute(
                            "mesaForm",
                            adminPisoMesaService
                                    .obtenerFormularioMesa(
                                            idMesaEditar
                                    )
                    );
                } catch (IllegalStateException e) {
                    model.addAttribute(
                            "error",
                            e.getMessage()
                    );
                    model.addAttribute(
                            "mesaForm",
                            adminPisoMesaService
                                    .nuevoFormularioMesa(idPiso)
                    );
                    idMesaEditar = null;
                }
            } else {
                model.addAttribute(
                        "mesaForm",
                        adminPisoMesaService
                                .nuevoFormularioMesa(idPiso)
                );
            }
        }

        model.addAttribute(
                "abrirModalPiso",
                "piso".equalsIgnoreCase(modal)
                        || idPisoEditar != null
        );

        model.addAttribute(
                "abrirModalMesa",
                "mesa".equalsIgnoreCase(modal)
                        || idMesaEditar != null
        );

        return "paginas/admin/pisos-mesas";
    }

    @GetMapping("/pisos/nuevo")
    public String nuevoPiso() {
        return "redirect:/admin/pisos-mesas?modal=piso";
    }

    @GetMapping("/pisos/{idPiso}/editar")
    public String editarPiso(
            @PathVariable Integer idPiso,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPisoMesaService
                    .obtenerFormularioPiso(idPiso);

            return "redirect:/admin/pisos-mesas"
                    + "?editarPiso=" + idPiso;

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

            redirectAttributes.addFlashAttribute(
                    "pisoForm",
                    formulario
            );

            if (formulario.getIdPiso() == null) {
                return "redirect:/admin/pisos-mesas"
                        + "?modal=piso";
            }

            return "redirect:/admin/pisos-mesas"
                    + "?editarPiso="
                    + formulario.getIdPiso();
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
            ) Integer idPiso
    ) {
        String destino =
                "redirect:/admin/pisos-mesas?modal=mesa";

        if (idPiso != null) {
            destino += "&piso=" + idPiso;
        }

        return destino;
    }

    @GetMapping("/mesas/{idMesa}/editar")
    public String editarMesa(
            @PathVariable Integer idMesa,
            RedirectAttributes redirectAttributes
    ) {
        try {
            AdminMesaFormDTO formulario =
                    adminPisoMesaService
                            .obtenerFormularioMesa(idMesa);

            return "redirect:/admin/pisos-mesas"
                    + "?editarMesa=" + idMesa
                    + "&piso=" + formulario.getIdPiso();

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

            redirectAttributes.addFlashAttribute(
                    "mesaForm",
                    formulario
            );

            String destino =
                    "redirect:/admin/pisos-mesas";

            if (formulario.getIdMesa() == null) {
                destino += "?modal=mesa";
            } else {
                destino += "?editarMesa="
                        + formulario.getIdMesa();
            }

            if (formulario.getIdPiso() != null) {
                destino += "&piso="
                        + formulario.getIdPiso();
            }

            return destino;
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