package com.rinconcito.controller;

import com.rinconcito.dto.ConfiguracionRestauranteFormDTO;
import com.rinconcito.dto.HorarioConfiguracionFormDTO;
import com.rinconcito.service.ConfiguracionImagenStorageService;
import com.rinconcito.service.ConfiguracionRestauranteService;
import com.rinconcito.service.HorarioAtencionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/admin/configuracion")
public class AdminConfiguracionController {

    private final ConfiguracionRestauranteService
            configuracionRestauranteService;

    private final ConfiguracionImagenStorageService
            configuracionImagenStorageService;

    private final HorarioAtencionService
            horarioAtencionService;

    public AdminConfiguracionController(
            ConfiguracionRestauranteService
                    configuracionRestauranteService,

            ConfiguracionImagenStorageService
                    configuracionImagenStorageService,

            HorarioAtencionService
                    horarioAtencionService
    ) {
        this.configuracionRestauranteService =
                configuracionRestauranteService;

        this.configuracionImagenStorageService =
                configuracionImagenStorageService;

        this.horarioAtencionService =
                horarioAtencionService;
    }

    /*
     * Muestra toda la configuración:
     *
     * - Datos generales.
     * - Contacto.
     * - Redes sociales.
     * - Medios de pago.
     * - Horarios de atención.
     */
    @GetMapping
    public String mostrarConfiguracion(
            Model model
    ) {
        prepararModelo(
                model,
                configuracionRestauranteService
                        .obtenerFormulario(),
                horarioAtencionService
                        .obtenerFormularioConfiguracion()
        );

        return "paginas/admin/configuracion";
    }

    /*
     * Guarda los datos generales y medios de pago.
     */
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("configuracionForm")
            ConfiguracionRestauranteFormDTO formulario,

            @RequestParam(
                    value = "archivoQrYape",
                    required = false
            )
            MultipartFile archivoQrYape,

            @RequestParam(
                    value = "archivoQrPlin",
                    required = false
            )
            MultipartFile archivoQrPlin,

            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String qrYapeAnterior =
                    formulario.getQrYapeActual();

            String qrPlinAnterior =
                    formulario.getQrPlinActual();

            configuracionRestauranteService.guardar(
                    formulario
            );

            /*
             * Reemplazar QR de Yape.
             */
            if (
                    archivoQrYape != null
                            && !archivoQrYape.isEmpty()
            ) {
                String nuevoQrYape =
                        configuracionImagenStorageService
                                .guardarQr(
                                        archivoQrYape,
                                        "YAPE"
                                );

                configuracionRestauranteService
                        .actualizarQrYape(
                                nuevoQrYape
                        );

                eliminarImagenAnterior(
                        qrYapeAnterior,
                        nuevoQrYape
                );
            }

            /*
             * Reemplazar QR de Plin.
             */
            if (
                    archivoQrPlin != null
                            && !archivoQrPlin.isEmpty()
            ) {
                String nuevoQrPlin =
                        configuracionImagenStorageService
                                .guardarQr(
                                        archivoQrPlin,
                                        "PLIN"
                                );

                configuracionRestauranteService
                        .actualizarQrPlin(
                                nuevoQrPlin
                        );

                eliminarImagenAnterior(
                        qrPlinAnterior,
                        nuevoQrPlin
                );
            }

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La configuración fue guardada correctamente."
            );

            return "redirect:/admin/configuracion";

        } catch (IllegalStateException e) {

            restaurarNombresQr(
                    formulario
            );

            prepararModelo(
                    model,
                    formulario,
                    horarioAtencionService
                            .obtenerFormularioConfiguracion()
            );

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "paginas/admin/configuracion";
        }
    }

    /*
     * Guarda los horarios de atención.
     */
    @PostMapping("/horarios/guardar")
    public String guardarHorarios(
            @ModelAttribute("horarioForm")
            HorarioConfiguracionFormDTO horarioForm,

            RedirectAttributes redirectAttributes
    ) {
        try {
            horarioAtencionService
                    .guardarConfiguracion(
                            horarioForm
                    );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Los horarios de atención fueron "
                            + "actualizados correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/configuracion";
    }

    /*
     * Elimina un código QR.
     */
    @PostMapping("/qr/{tipo}/eliminar")
    public String eliminarQr(
            @PathVariable String tipo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String tipoNormalizado =
                    tipo.trim()
                            .toUpperCase(Locale.ROOT);

            ConfiguracionRestauranteFormDTO formulario =
                    configuracionRestauranteService
                            .obtenerFormulario();

            if ("YAPE".equals(tipoNormalizado)) {

                String qrAnterior =
                        formulario.getQrYapeActual();

                configuracionRestauranteService
                        .actualizarQrYape(null);

                configuracionImagenStorageService
                        .eliminarSiExiste(
                                qrAnterior
                        );

            } else if ("PLIN".equals(tipoNormalizado)) {

                String qrAnterior =
                        formulario.getQrPlinActual();

                configuracionRestauranteService
                        .actualizarQrPlin(null);

                configuracionImagenStorageService
                        .eliminarSiExiste(
                                qrAnterior
                        );

            } else {

                throw new IllegalStateException(
                        "El tipo de código QR no es válido."
                );
            }

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "El código QR fue eliminado correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/configuracion";
    }

    private void prepararModelo(
            Model model,
            ConfiguracionRestauranteFormDTO
                    configuracionForm,
            HorarioConfiguracionFormDTO
                    horarioForm
    ) {
        model.addAttribute(
                "titulo",
                "Configuración del restaurante"
        );

        model.addAttribute(
                "configuracionForm",
                configuracionForm
        );

        model.addAttribute(
                "horarioForm",
                horarioForm
        );
    }

    private void eliminarImagenAnterior(
            String anterior,
            String nuevo
    ) {
        if (
                anterior == null
                        || anterior.isBlank()
                        || anterior.equals(nuevo)
        ) {
            return;
        }

        configuracionImagenStorageService
                .eliminarSiExiste(
                        anterior
                );
    }

    private void restaurarNombresQr(
            ConfiguracionRestauranteFormDTO formulario
    ) {
        try {
            ConfiguracionRestauranteFormDTO actual =
                    configuracionRestauranteService
                            .obtenerFormulario();

            formulario.setQrYapeActual(
                    actual.getQrYapeActual()
            );

            formulario.setQrPlinActual(
                    actual.getQrPlinActual()
            );

            if (
                    formulario.getIdConfiguracion()
                            == null
            ) {
                formulario.setIdConfiguracion(
                        actual.getIdConfiguracion()
                );
            }

        } catch (Exception ignored) {
        }
    }
}