package com.rinconcito.controller;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CartaController {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProductoService productoService;

    @GetMapping("/carta")
    public String carta(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoria", required = false) Integer idCategoria,
            Model model) {

        List<Categoria> categorias = categoriaService.listarActivas();
        List<Producto> productos = productoService.buscarPublicos(q, idCategoria);

        model.addAttribute("titulo", "Carta");
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", idCategoria);
        model.addAttribute("totalProductos", productos.size());
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
