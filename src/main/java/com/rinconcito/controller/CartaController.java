package com.rinconcito.controller;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
import com.rinconcito.service.CarritoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CartaController {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CarritoService carritoService;

    @GetMapping("/carta")
    public String carta(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoria", required = false) Integer idCategoria,
            HttpSession session,
            Model model) {

        List<Categoria> categorias = categoriaService.listarActivas();
        List<Producto> productos = productoService.buscarPublicos(q, idCategoria);

        Map<Categoria, List<Producto>> menu = new LinkedHashMap<>();
        for (Producto producto : productos) {
            menu.computeIfAbsent(producto.getCategoria(), c -> new java.util.ArrayList<>()).add(producto);
        }

        model.addAttribute("titulo", "Carta");
        model.addAttribute("menu", menu);
        model.addAttribute("categorias", categorias);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", idCategoria);
        model.addAttribute("totalProductos", productos.size());
        model.addAttribute("itemsCarrito", carritoService.obtenerItems(session));
        model.addAttribute("subtotalCarrito", carritoService.subtotal(session));
        return "paginas/carta";
    }

    @GetMapping("/carta/{id}")
    public String detalle(
            @PathVariable("id") Integer id,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoria", required = false) Integer idCategoria,
            Model model) {
        Producto producto = productoService.buscarPublicoPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));

        List<Producto> relacionados = productoService.recomendarPublicos(id);

        model.addAttribute("titulo", producto.getNombre());
        model.addAttribute("producto", producto);
        model.addAttribute("relacionados", relacionados);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", idCategoria);
        return "paginas/producto-detalle";
    }
}
