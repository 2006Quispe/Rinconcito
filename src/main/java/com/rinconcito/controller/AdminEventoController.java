package com.rinconcito.controller;

import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import com.rinconcito.service.EventoImagenStorageService;
import com.rinconcito.service.EventoService;
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

@Controller
@RequestMapping("/admin/eventos")
public class AdminEventoController {

    private static final String VISTA_LISTA =
            "paginas/admin/eventos/lista";

    private static final String RUTA_IMAGENES_SUBIDAS =
            "/eventos/imagenes/";

    private final EventoService eventoService;
    private final EventoImagenStorageService imagenStorage;

    public AdminEventoController(
            EventoService eventoService,
            EventoImagenStorageService imagenStorage
    ) {
        this.eventoService =
                eventoService;
        this.imagenStorage =
                imagenStorage;
    }

    @GetMapping
    public String listar(
            Model model
    ) {
        return prepararLista(
                model,
                eventoNuevo(),
                false
        );
    }

    /**
     * Abre la lista con el panel desplegable de
     * "Información del evento" listo para un evento nuevo.
     */
    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {
        return prepararLista(
                model,
                eventoNuevo(),
                true
        );
    }

    /**
     * Abre la lista con el panel desplegable cargado
     * con los datos del evento a editar.
     */
    @GetMapping("/{idEvento}/editar")
    public String editar(
            @PathVariable Integer idEvento,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            Evento evento =
                    eventoService.buscarPorId(
                            idEvento
                    );

            return prepararLista(
                    model,
                    evento,
                    true
            );

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/eventos";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Evento evento,
            @RequestParam(value = "archivoImagen", required = false)
            MultipartFile archivoImagen,
            @RequestParam(value = "quitarImagen", defaultValue = "false")
            boolean quitarImagen,
            Model model,
            RedirectAttributes ra
    ) {
        // Imagen que tenía el evento antes de este envío.
        String imagenAnterior = evento.getImagen();
        String imagenNueva = null;

        try {
            boolean nuevo =
                    evento.getIdEvento() == null;

            if (archivoImagen != null
                    && !archivoImagen.isEmpty()) {
                imagenNueva =
                        imagenStorage.guardar(archivoImagen);
                evento.setImagen(imagenNueva);
            } else if (quitarImagen) {
                evento.setImagen(null);
            }

            eventoService.guardar(evento);

            // Ya guardado: se borra el archivo anterior solo si
            // lo subió el sistema y fue reemplazado o quitado.
            if (imagenAnterior != null
                    && imagenAnterior.startsWith(
                            RUTA_IMAGENES_SUBIDAS)
                    && !imagenAnterior.equals(
                            evento.getImagen())) {
                imagenStorage.eliminar(imagenAnterior);
            }

            ra.addFlashAttribute(
                    "exito",
                    nuevo
                            ? "Evento registrado correctamente."
                            : "Evento actualizado correctamente."
            );

            return "redirect:/admin/eventos";

        } catch (IllegalStateException e) {

            // Si falló, no se deja huérfana la imagen recién subida.
            if (imagenNueva != null) {
                imagenStorage.eliminar(imagenNueva);
            }
            evento.setImagen(imagenAnterior);

            // Se vuelve a mostrar la lista con el modal abierto
            // y con lo que el administrador ya había escrito.
            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return prepararLista(
                    model,
                    evento,
                    true
            );
        }
    }

    @PostMapping("/{idEvento}/estado")
    public String cambiarEstado(
            @PathVariable Integer idEvento,
            @RequestParam EstadoEvento estado,
            RedirectAttributes ra
    ) {
        try {
            eventoService.cambiarEstado(
                    idEvento,
                    estado
            );

            ra.addFlashAttribute(
                    "exito",
                    "Estado del evento actualizado."
            );

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/eventos";
    }

    @PostMapping("/{idEvento}/eliminar")
    public String eliminar(
            @PathVariable Integer idEvento,
            RedirectAttributes ra
    ) {
        try {
            String imagen =
                    eventoService.eliminar(idEvento);

            // Solo se borra del disco si la subió el sistema.
            if (imagen != null
                    && imagen.startsWith(
                            RUTA_IMAGENES_SUBIDAS)) {
                imagenStorage.eliminar(imagen);
            }

            ra.addFlashAttribute(
                    "exito",
                    "Evento eliminado correctamente."
            );

        } catch (IllegalStateException e) {

            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/eventos";
    }

    private Evento eventoNuevo() {
        Evento evento =
                new Evento();

        evento.setEstado(
                EstadoEvento.ACTIVO
        );

        evento.setPrecioEntrada(0);

        return evento;
    }

    private String prepararLista(
            Model model,
            Evento evento,
            boolean panelAbierto
    ) {
        model.addAttribute(
                "titulo",
                "Gestión de eventos"
        );

        model.addAttribute(
                "eventos",
                eventoService.listarTodos()
        );

        model.addAttribute(
                "evento",
                evento
        );

        model.addAttribute(
                "estados",
                EstadoEvento.values()
        );

        model.addAttribute(
                "panelAbierto",
                panelAbierto
        );

        return VISTA_LISTA;
    }
}
