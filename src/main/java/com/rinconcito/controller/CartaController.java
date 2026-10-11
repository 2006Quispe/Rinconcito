package com.rinconcito.controller;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
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

    @GetMapping("/carta")
    public String carta(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoria", required = false) Integer idCategoria,
            Model model) {

        List<Categoria> categorias = categoriaService.listarActivas();
        List<Producto> productos = productoService.buscarPublicos(q, idCategoria);

        Map<Categoria, List<Producto>> menu = new LinkedHashMap<>();
        if (idCategoria != null || (q != null && !q.isBlank())) {
            for (Producto producto : productos) {
                menu.computeIfAbsent(producto.getCategoria(), c -> new java.util.ArrayList<>()).add(producto);
            }
        } else {
            for (Categoria categoria : categorias) {
                menu.put(categoria, productoService.listarPorCategoria(categoria));
            }
        }

        model.addAttribute("titulo", "Carta");
        model.addAttribute("menu", menu);
        model.addAttribute("categorias", categorias);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", idCategoria);
        return "paginas/carta";
    }
}
