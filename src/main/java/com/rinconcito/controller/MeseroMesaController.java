package com.rinconcito.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rinconcito.dto.MeseroPisoDTO;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.ReservaRepository;
import com.rinconcito.service.MeseroMesaService;

@Controller
@RequestMapping("/mesero")
public class MeseroMesaController {

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final List<EstadoReserva>
            ESTADOS_RESERVA_OPERATIVOS =
            List.of(
                    EstadoReserva.CONFIRMADA,
                    EstadoReserva.CLIENTE_LLEGO,
                    EstadoReserva.EN_ATENCION
            );

    private final MeseroMesaService
            meseroMesaService;

    private final ReservaRepository
            reservaRepository;

    public MeseroMesaController(
            MeseroMesaService meseroMesaService,
            ReservaRepository reservaRepository
    ) {
        this.meseroMesaService =
                meseroMesaService;

        this.reservaRepository =
                reservaRepository;
    }

    /*
     * Panel principal de control de mesas.
     */
    @GetMapping("/mesas")
    public String mostrarMesas(
            Model model
    ) {
        List<MeseroPisoDTO> pisos =
                meseroMesaService
                        .listarPisosConMesasHoy();

        model.addAttribute(
                "titulo",
                "Control de mesas"
        );

        model.addAttribute(
                "pisos",
                pisos
        );

        model.addAttribute(
                "fechaActual",
                obtenerFechaActualTexto()
        );

        return "paginas/mesero/mesas";
    }

    /*
     * Muestra las reservas que deben atenderse
     * durante el día actual.
     */
    
@GetMapping("/reservas")
public String mostrarReservasDelDia(
        @RequestParam(value = "fecha", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fecha,

        @RequestParam(value = "codigo", required = false)
        String codigo,

        Model model
) {
    LocalDate hoy = obtenerFechaActual();

    LocalDate fechaSeleccionada =
            fecha != null ? fecha : hoy;

    String codigoBusqueda =
            codigo != null ? codigo.trim() : "";

    List<Reserva> reservas;

    // Buscar directamente por código.
    if (!codigoBusqueda.isBlank()) {

        reservas = reservaRepository
                .buscarPorCodigoParaMesero(
                        codigoBusqueda,
                        ESTADOS_RESERVA_OPERATIVOS
                )
                .map(List::of)
                .orElseGet(List::of);

        // Si encuentra la reserva, mostrar su fecha real.
        if (!reservas.isEmpty()) {
            fechaSeleccionada =
                    reservas.get(0).getFechaReserva();
        }

    } else {

        // Buscar todas las reservas de la fecha elegida.
        reservas = reservaRepository
                .listarReservasOperativasDelDia(
                        fechaSeleccionada,
                        ESTADOS_RESERVA_OPERATIVOS
                );
    }

    long totalConfirmadas = reservas.stream()
            .filter(reserva ->
                    reserva.getEstado()
                            == EstadoReserva.CONFIRMADA
            )
            .count();

    long totalLlegaron = reservas.stream()
            .filter(reserva ->
                    reserva.getEstado()
                            == EstadoReserva.CLIENTE_LLEGO
            )
            .count();

    long totalEnAtencion = reservas.stream()
            .filter(reserva ->
                    reserva.getEstado()
                            == EstadoReserva.EN_ATENCION
            )
            .count();

    model.addAttribute("titulo", "Reservas");

    model.addAttribute("fechaActual", hoy);

    model.addAttribute(
            "fechaSeleccionada",
            fechaSeleccionada
    );

    model.addAttribute("codigoBusqueda", codigoBusqueda);

    model.addAttribute(
            "esHoy",
            hoy.equals(fechaSeleccionada)
    );

    model.addAttribute("reservas", reservas);

    model.addAttribute("totalReservas", reservas.size());

    model.addAttribute(
            "totalConfirmadas",
            totalConfirmadas
    );

    model.addAttribute("totalLlegaron", totalLlegaron);

    model.addAttribute(
            "totalEnAtencion",
            totalEnAtencion
    );

    return "paginas/mesero/reservas";
}


    /*
     * Ocupa manualmente una mesa para clientes
     * que llegaron sin reserva.
     */
    @PostMapping("/mesas/{idMesa}/ocupar")
    public String ocuparMesa(
            @PathVariable
            Integer idMesa,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meseroMesaService.ocuparMesa(
                    idMesa
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La mesa fue marcada como ocupada."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/mesas";
    }

    /*
     * Libera manualmente una mesa ocupada.
     */
    @PostMapping("/mesas/{idMesa}/liberar")
    public String liberarMesa(
            @PathVariable
            Integer idMesa,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meseroMesaService.liberarMesa(
                    idMesa
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La mesa fue liberada correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/mesas";
    }

    /*
     * Registra la llegada del cliente:
     *
     * CONFIRMADA -> CLIENTE_LLEGO
     */
    @PostMapping(
            "/reservas/{idReserva}/llegada"
    )
    public String registrarLlegada(
            @PathVariable
            Integer idReserva,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meseroMesaService.registrarLlegada(
                    idReserva
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Llegada del cliente registrada. "
                    + "La mesa ahora está ocupada."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/reservas";
    }

    /*
     * Inicia formalmente la atención:
     *
     * CLIENTE_LLEGO -> EN_ATENCION
     */
    @PostMapping(
            "/reservas/{idReserva}/iniciar"
    )
    public String iniciarAtencion(
            @PathVariable
            Integer idReserva,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meseroMesaService.iniciarAtencion(
                    idReserva
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La atención de la reserva fue iniciada."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/reservas";
    }

    /*
     * Finaliza la atención:
     *
     * EN_ATENCION -> FINALIZADA
     */
    @PostMapping(
            "/reservas/{idReserva}/finalizar"
    )
    public String finalizarReserva(
            @PathVariable
            Integer idReserva,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meseroMesaService.finalizarReserva(
                    idReserva
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "La reserva fue finalizada correctamente."
            );

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/mesero/reservas";
    }

    private LocalDate obtenerFechaActual() {
        return LocalDate.now(
                ZONA_PERU
        );
    }

    private String obtenerFechaActualTexto() {

        LocalDate hoy =
                obtenerFechaActual();

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "EEEE d 'de' MMMM 'de' yyyy",
                        new Locale("es", "PE")
                );

        String fecha =
                hoy.format(formato);

        if (fecha.isBlank()) {
            return "";
        }

        return fecha.substring(0, 1)
                .toUpperCase()
                + fecha.substring(1);
    }
}