package com.rinconcito.config;

import com.rinconcito.dto.HorarioResumenDTO;
import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.service.CarritoService;
import com.rinconcito.service.ConfiguracionRestauranteService;
import com.rinconcito.service.HorarioAtencionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class DatosPublicosAdvice {

    @Autowired
    private ConfiguracionRestauranteService configuracionRestauranteService;

    @Autowired
    private HorarioAtencionService horarioAtencionService;

    @Autowired
    private CarritoService carritoService;

    @ModelAttribute("configuracion")
    public ConfiguracionRestaurante configuracion() {
        return configuracionRestauranteService.obtener();
    }

    @ModelAttribute("resumenHorarios")
    public List<HorarioResumenDTO> resumenHorarios() {
        return horarioAtencionService.resumir();
    }

    @ModelAttribute("carritoCount")
    public int carritoCount(HttpSession session) {
        return carritoService.contarUnidades(session);
    }
}