package com.rinconcito.controller;

import com.rinconcito.dto.ItemPedidoDTO;
import com.rinconcito.dto.PedidoFormDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.service.PedidoService;
import com.rinconcito.service.ProductoService;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @GetMapping("/pedidos")
    public String mostrarFormulario(Model model) {
        List<Producto> productos = productoRepository.findByEstado(EstadoProducto.ACTIVO);

        PedidoFormDTO form = new PedidoFormDTO();
        for (Producto producto : productos) {
            ItemPedidoDTO item = new ItemPedidoDTO();
            item.setIdProducto(producto.getIdProducto());
            item.setCantidad(0);
            form.getItems().add(item);
        }

        model.addAttribute("titulo", "Pedidos");
        model.addAttribute("productos", productos);
        model.addAttribute("pedidoForm", form);
        return "paginas/pedidos";
    }

    @PostMapping("/pedidos")
    public String procesarPedido(@ModelAttribute("pedidoForm") PedidoFormDTO pedidoForm, Model model) {
        model.addAttribute("titulo", "Pedidos");

        try {
            Pedido pedido = pedidoService.crearPedido(pedidoForm);
            model.addAttribute("exito", true);
            model.addAttribute("pedido", pedido);
            pagoRepository.findByPedido(pedido).ifPresent(pago -> model.addAttribute("pago", pago));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pedidoForm", pedidoForm);
            model.addAttribute("productos", productoRepository.findByEstado(EstadoProducto.ACTIVO));
        }

        return "paginas/pedidos";
    }
}