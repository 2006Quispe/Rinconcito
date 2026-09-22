package com.rinconcito.controller;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Reserva;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PagoRepository pagoRepository;

    @GetMapping("/reservas")
    public String mostrarFormulario(Model model) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("reservaForm", new ReservaFormDTO());
        return "paginas/reservas";
    }

    @PostMapping("/reservas")
    public String procesarReserva(@ModelAttribute("reservaForm") ReservaFormDTO reservaForm, Model model) {
        model.addAttribute("titulo", "Reservas");

        try {
            Reserva reserva = reservaService.crearReserva(reservaForm);
            model.addAttribute("exito", true);
            model.addAttribute("reserva", reserva);

            pagoRepository.findByReserva(reserva).ifPresent(pago -> model.addAttribute("pago", pago));

        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reservaForm", reservaForm);
        }

        return "paginas/reservas";
    }
}