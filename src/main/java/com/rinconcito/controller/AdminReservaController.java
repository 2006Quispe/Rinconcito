package com.rinconcito.controller;

import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PisoRepository pisoRepository;

    @GetMapping("/reservas")
    public String listarReservas(
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "idPiso", required = false) Integer idPiso,
            @RequestParam(value = "estado", required = false) EstadoReserva estado,
            Model model) {

        model.addAttribute("titulo", "Admin · Reservas");
        model.addAttribute("reservas", reservaService.listarConFiltros(fecha, idPiso, estado));
        model.addAttribute("pisos", pisoRepository.findByEstado(EstadoPiso.ACTIVO));
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("filtroFecha", fecha);
        model.addAttribute("filtroIdPiso", idPiso);
        model.addAttribute("filtroEstado", estado);
        return "paginas/admin/reservas";
    }
}
