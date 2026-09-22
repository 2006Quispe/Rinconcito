package com.rinconcito.controller;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String carta(Model model) {
        List<Categoria> categorias = categoriaService.listarActivas();

        Map<Categoria, List<Producto>> menu = new LinkedHashMap<>();
        for (Categoria categoria : categorias) {
            menu.put(categoria, productoService.listarPorCategoria(categoria));
        }

        model.addAttribute("titulo", "Carta");
        model.addAttribute("menu", menu);
        return "paginas/carta";
    }
}