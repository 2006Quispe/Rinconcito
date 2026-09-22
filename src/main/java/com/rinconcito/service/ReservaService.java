package com.rinconcito.service;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.*;
import com.rinconcito.entity.Evento.EstadoEvento;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.EventoRepository;
import com.rinconcito.repository.PagoRepository;
import com.rinconcito.repository.ReservaRepository;
import com.rinconcito.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservaService {

    private static final int DURACION_HORAS = 2;
    private static final int MAX_RESERVAS_POR_DIA = 1;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    public Reserva crearReserva(ReservaFormDTO dto) {

        LocalDate hoy = LocalDate.now();

        if (dto.getFecha().isBefore(hoy)) {
            throw new IllegalStateException("No puedes reservar para una fecha que ya pasó.");
        }
        if (dto.getFecha().isEqual(hoy) && dto.getHoraInicio().isBefore(LocalTime.now())) {
            throw new IllegalStateException("No puedes reservar para una hora que ya pasó hoy.");
        }

        Optional<Usuario> clienteExistente = usuarioRepository.findByDni(dto.getDni());
        if (clienteExistente.isPresent()) {
            long reservasActivas = reservaRepository
                    .findByClienteAndFechaReserva(clienteExistente.get(), dto.getFecha())
                    .stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                    .count();

            if (reservasActivas >= MAX_RESERVAS_POR_DIA) {
                throw new IllegalStateException(
                        "Ya tienes una reserva registrada para ese día. Si necesitas cambiarla, cancela la anterior primero.");
            }
        }

        boolean esFinDeSemana = dto.getFecha().getDayOfWeek() == DayOfWeek.SATURDAY
                || dto.getFecha().getDayOfWeek() == DayOfWeek.SUNDAY;

        Evento evento = null;
        if (esFinDeSemana) {
            List<Evento> eventosDelDia = eventoRepository.findByFechaAndEstado(dto.getFecha(), EstadoEvento.ACTIVO);
            if (eventosDelDia.isEmpty()) {
                throw new IllegalStateException(
                        "No hay ningún evento programado para ese día, por ahora no se puede reservar.");
            }
            evento = eventosDelDia.get(0);
        }

        LocalTime horaFin = dto.getHoraInicio().plusHours(DURACION_HORAS);

        Optional<Mesa> mesaDisponible = mesaService.buscarMesaDisponible(
                dto.getFecha(), dto.getHoraInicio(), horaFin, dto.getNumeroPersonas());

        if (mesaDisponible.isEmpty()) {
            throw new IllegalStateException(
                    "No hay mesas disponibles para " + dto.getNumeroPersonas() +
                    " personas en ese horario. Intenta con otra fecha u hora.");
        }

        Usuario cliente = usuarioService.buscarOCrearInvitado(
                dto.getDni(), dto.getNombres(), dto.getApellidos(), dto.getTelefono());

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setMesa(mesaDisponible.get());
        reserva.setFechaReserva(dto.getFecha());
        reserva.setHoraInicio(dto.getHoraInicio());
        reserva.setHoraFin(horaFin);
        reserva.setNumeroPersonas(dto.getNumeroPersonas());
        reserva.setOcasionEspecial(dto.getOcasionEspecial());
        reserva.setComentarios(dto.getComentarios());
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        reserva = reservaRepository.save(reserva);

        if (esFinDeSemana) {
            int monto = evento.getPrecioEntrada() * dto.getNumeroPersonas();

            Pago pago = new Pago();
            pago.setReserva(reserva);
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setMonto(monto);
            pago.setNumeroOperacion("SIMULADO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            pago.setEstado(EstadoPago.PAGADO);

            pagoRepository.save(pago);
        }

        return reserva;
    }
}