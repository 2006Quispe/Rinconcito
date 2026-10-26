package com.rinconcito.controller;

import com.rinconcito.service.CarritoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CarritoGlobalControllerAdvice {

    private final CarritoService carritoService;

    public CarritoGlobalControllerAdvice(
            CarritoService carritoService
    ) {
        this.carritoService = carritoService;
    }

    @ModelAttribute("carritoCount")
    public int obtenerCantidadCarrito(
            HttpSession session
    ) {
        return carritoService.contarUnidades(session);
    }

    /*
     * El botón flotante no debe mostrarse dentro del
     * carrito ni durante el proceso de completar el pedido.
     */
    @ModelAttribute("mostrarCarritoFlotante")
    public boolean mostrarCarritoFlotante(
            HttpServletRequest request
    ) {
        String ruta = request.getRequestURI();
        String contexto = request.getContextPath();

        if (
                contexto != null
                        && !contexto.isBlank()
                        && ruta.startsWith(contexto)
        ) {
            ruta = ruta.substring(contexto.length());
        }

        return !ruta.equals("/carrito")
                && !ruta.startsWith("/carrito/")
                && !ruta.equals("/pedidos")
                && !ruta.startsWith("/pedidos/");
    }

}