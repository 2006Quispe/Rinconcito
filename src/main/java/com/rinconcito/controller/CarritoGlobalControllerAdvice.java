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

}