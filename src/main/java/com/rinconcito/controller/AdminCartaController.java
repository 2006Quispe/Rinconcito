package com.rinconcito.controller;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.HistorialDisponibilidadProductoService;
import com.rinconcito.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminCartaController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private HistorialDisponibilidadProductoService
            historialDisponibilidadProductoService;

    @GetMapping("/carta")
    public String listarProductos(
            @RequestParam(value = "modal", required = false)
            String modal,
            @RequestParam(value = "editar", required = false)
            Integer idProductoEditar,
            Model model
    ) {
        model.addAttribute("titulo", "Admin · Carta");
        model.addAttribute(
                "productos",
                productoService.listarTodosAdmin()
        );
        model.addAttribute(
                "categorias",
                categoriaService.listarActivas()
        );
        model.addAttribute(
                "abrirModalNuevo",
                "nuevo".equalsIgnoreCase(modal)
        );
        model.addAttribute(
                "abrirModalEditar",
                false
        );

        if (idProductoEditar != null) {
            productoService.buscarPorId(idProductoEditar)
                    .ifPresentOrElse(
                            producto -> {
                                model.addAttribute(
                                        "productoEditar",
                                        producto
                                );
                                model.addAttribute(
                                        "abrirModalEditar",
                                        true
                                );
                            },
                            () -> model.addAttribute(
                                    "error",
                                    "Producto no encontrado."
                            )
                    );
        }

        return "paginas/admin/carta";
    }

    @GetMapping("/carta/historial-disponibilidad")
    public String historialDisponibilidad(
            Model model
    ) {
        model.addAttribute(
                "titulo",
                "Historial de disponibilidad"
        );

        model.addAttribute(
                "historial",
                historialDisponibilidadProductoService
                        .listarRecientes()
        );

        return "paginas/admin/historial-disponibilidad";
    }

    @GetMapping("/carta/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("titulo", "Nuevo producto");
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("estados", EstadoProducto.values());
        return "paginas/admin/producto-form";
    }

    @GetMapping("/carta/{id}/editar")
    public String editarProducto(
            @PathVariable("id") Integer id,
            RedirectAttributes ra
    ) {
        if (productoService.buscarPorId(id).isEmpty()) {
            ra.addFlashAttribute(
                    "error",
                    "Producto no encontrado."
            );
            return "redirect:/admin/carta";
        }

        return "redirect:/admin/carta?editar=" + id;
    }

    @PostMapping("/carta/guardar")
    public String guardarProducto(
            @RequestParam(value = "idProducto",required = false)
            Integer idProducto,
            @RequestParam("nombre")String nombre,
            @RequestParam(value = "descripcion",required = false)
            String descripcion,
            @RequestParam("precio")Integer precio,
            @RequestParam("idCategoria")Integer idCategoria,
            @RequestParam(value = "disponible",defaultValue = "false")
            boolean disponible,

            @RequestParam(value = "estado",defaultValue = "ACTIVO")
            EstadoProducto estado,
            @RequestParam(value = "archivoImagen",required = false)
            org.springframework.web.multipart.MultipartFile archivoImagen,
            @RequestParam(value = "eliminarImagen",defaultValue = "false")
            boolean eliminarImagen,
            
            RedirectAttributes ra) {
        try {
            Producto producto;

            if (idProducto == null) {
                producto = new Producto();
            } else {
                producto = productoService
                        .buscarPorId(idProducto)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Producto no encontrado."
                                )
                        );
            }

            producto.setNombre(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecio(precio);
            producto.setDisponible(disponible);
            producto.setEstado(estado);

            productoService.guardar(
                    producto,
                    idCategoria,
                    archivoImagen,
                    eliminarImagen
            );

            ra.addFlashAttribute(
                    "exito",
                    "Producto guardado correctamente."
            );

            return "redirect:/admin/carta";

        } catch (IllegalStateException e) {
            ra.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            if (idProducto != null) {
                return "redirect:/admin/carta?editar="
                        + idProducto;
            }
            return "redirect:/admin/carta?modal=nuevo";
        }
    }

    @PostMapping("/carta/{id}/estado")
    public String cambiarEstadoProducto(
            @PathVariable("id") Integer id,
            @RequestParam("estado") EstadoProducto estado,
            RedirectAttributes ra) {
        try {
            productoService.cambiarEstado(id, estado);
            ra.addFlashAttribute("exito",
                    estado == EstadoProducto.ACTIVO ? "Producto activado." : "Producto desactivado (soft-delete).");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/carta";
    }

    // ---- Categorías ----

    @GetMapping("/categorias")
    public String listarCategorias(Model model) {
        model.addAttribute("titulo", "Admin · Categorías");
        model.addAttribute("categorias", categoriaService.listarTodasAdmin());
        return "paginas/admin/categorias";
    }

    @GetMapping("/categorias/nueva")
    public String nuevaCategoria(Model model) {
        model.addAttribute("titulo", "Nueva categoría");
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("estados", EstadoCategoria.values());
        return "paginas/admin/categoria-form";
    }

    @GetMapping("/categorias/{id}/editar")
    public String editarCategoria(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
        return categoriaService.buscarPorId(id)
                .map(categoria -> {
                    model.addAttribute("titulo", "Editar categoría");
                    model.addAttribute("categoria", categoria);
                    model.addAttribute("estados", EstadoCategoria.values());
                    return "paginas/admin/categoria-form";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Categoría no encontrada.");
                    return "redirect:/admin/categorias";
                });
    }

    @PostMapping("/categorias/guardar")
    public String guardarCategoria(
            @RequestParam(value = "idCategoria", required = false) Integer idCategoria,
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "estado", defaultValue = "ACTIVO") EstadoCategoria estado,
            RedirectAttributes ra) {

        try {
            Categoria categoria = idCategoria == null
                    ? new Categoria()
                    : categoriaService.buscarPorId(idCategoria)
                    .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));
            categoria.setNombre(nombre);
            categoria.setDescripcion(descripcion);
            categoria.setEstado(estado);
            categoriaService.guardar(categoria);
            ra.addFlashAttribute("exito", "Categoría guardada correctamente.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (idCategoria != null) {
                return "redirect:/admin/categorias/" + idCategoria + "/editar";
            }
            return "redirect:/admin/categorias/nueva";
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/estado")
    public String cambiarEstadoCategoria(
            @PathVariable("id") Integer id,
            @RequestParam("estado") EstadoCategoria estado,
            RedirectAttributes ra) {
        try {
            categoriaService.cambiarEstado(id, estado);
            ra.addFlashAttribute("exito",
                    estado == EstadoCategoria.ACTIVO ? "Categoría activada." : "Categoría desactivada (soft-delete).");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }
}
