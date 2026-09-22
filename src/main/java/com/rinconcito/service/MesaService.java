package com.rinconcito.service;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Reserva;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class MesaService {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    public Optional<Mesa> buscarMesaDisponible(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int numeroPersonas) {
        List<Mesa> candidatas = mesaRepository.findByEstado(EstadoMesa.DISPONIBLE);

        return candidatas.stream()
                .filter(mesa -> mesa.getCapacidad() >= numeroPersonas)
                .filter(mesa -> !hayCruceDeHorario(mesa, fecha, horaInicio, horaFin))
                .sorted((a, b) -> Integer.compare(a.getCapacidad(), b.getCapacidad()))
                .findFirst();
    }

    private boolean hayCruceDeHorario(Mesa mesa, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        List<Reserva> reservasDelDia = reservaRepository.findByMesaAndFechaReserva(mesa, fecha);

        for (Reserva reserva : reservasDelDia) {
            if (reserva.getEstado() == Reserva.EstadoReserva.CANCELADA) {
                continue;
            }

            boolean seCruzan = horaInicio.isBefore(reserva.getHoraFin()) && horaFin.isAfter(reserva.getHoraInicio());
            if (seCruzan) {
                return true;
            }
        }
        return false;
    }
}