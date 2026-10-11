package com.rinconcito.service;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.repository.MesaRepository;
import com.rinconcito.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MesaService {

    private static final Set<EstadoReserva>
            ESTADOS_QUE_OCUPAN_MESA = EnumSet.of(
                    EstadoReserva.CONFIRMADA,
                    EstadoReserva.CLIENTE_LLEGO,
                    EstadoReserva.EN_ATENCION
            );

    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;

    public MesaService(
            MesaRepository mesaRepository,
            ReservaRepository reservaRepository
    ) {
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Mesa> buscarMesaDisponible(
            Piso piso,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            int numeroPersonas
    ) {
        validarDatosBusqueda(
                piso,
                fecha,
                horaInicio,
                horaFin,
                numeroPersonas
        );

        if (piso.getEstado() != EstadoPiso.ACTIVO) {
            return Optional.empty();
        }

        List<Mesa> candidatas =
                mesaRepository
                        .findByPisoAndEstadoNot(
                                piso,
                                EstadoMesa.INACTIVA
                        )
                        .stream()
                        .filter(mesa ->
                                mesa.getCapacidad() != null
                        )
                        .filter(mesa ->
                                mesa.getCapacidad()
                                        >= numeroPersonas
                        )
                        .filter(mesa ->
                                !hayCruceDeHorario(
                                        mesa,
                                        fecha,
                                        horaInicio,
                                        horaFin
                                )
                        )
                        .toList();

        return elegirMesaAleatoriaAdecuada(
                candidatas
        );
    }

    @Transactional(readOnly = true)
    public Optional<Mesa> buscarMesaDisponible(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            int numeroPersonas
    ) {
        validarDatosBusqueda(
                null,
                fecha,
                horaInicio,
                horaFin,
                numeroPersonas
        );

        List<Mesa> candidatas =
                mesaRepository.findAll()
                        .stream()
                        .filter(mesa ->
                                mesa.getPiso() != null
                        )
                        .filter(mesa ->
                                mesa.getPiso().getEstado()
                                        == EstadoPiso.ACTIVO
                        )
                        .filter(mesa ->
                                mesa.getEstado()
                                        != EstadoMesa.INACTIVA
                        )
                        .filter(mesa ->
                                mesa.getCapacidad() != null
                        )
                        .filter(mesa ->
                                mesa.getCapacidad()
                                        >= numeroPersonas
                        )
                        .filter(mesa ->
                                !hayCruceDeHorario(
                                        mesa,
                                        fecha,
                                        horaInicio,
                                        horaFin
                                )
                        )
                        .toList();

        return elegirMesaAleatoriaAdecuada(
                candidatas
        );
    }

    private Optional<Mesa> elegirMesaAleatoriaAdecuada(
            List<Mesa> candidatas
    ) {
        if (candidatas == null
                || candidatas.isEmpty()) {

            return Optional.empty();
        }

        int capacidadMinima =
                candidatas.stream()
                        .mapToInt(Mesa::getCapacidad)
                        .min()
                        .orElseThrow();

        List<Mesa> mesasAdecuadas =
                candidatas.stream()
                        .filter(mesa ->
                                mesa.getCapacidad()
                                        == capacidadMinima
                        )
                        .toList();

        int indiceAleatorio =
                ThreadLocalRandom.current()
                        .nextInt(
                                mesasAdecuadas.size()
                        );

        return Optional.of(
                mesasAdecuadas.get(
                        indiceAleatorio
                )
        );
    }

    private boolean hayCruceDeHorario(
            Mesa mesa,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        List<Reserva> reservasDelDia =
                reservaRepository
                        .findByMesaAndFechaReserva(
                                mesa,
                                fecha
                        );

        for (Reserva reserva : reservasDelDia) {

            if (!ESTADOS_QUE_OCUPAN_MESA.contains(
                    reserva.getEstado()
            )) {
                continue;
            }

            if (reserva.getHoraInicio() == null
                    || reserva.getHoraFin() == null) {
                continue;
            }

            boolean seCruzan =
                    horaInicio.isBefore(
                            reserva.getHoraFin()
                    )
                    && horaFin.isAfter(
                            reserva.getHoraInicio()
                    );

            if (seCruzan) {
                return true;
            }
        }

        return false;
    }

    private void validarDatosBusqueda(
            Piso piso,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            int numeroPersonas
    ) {
        if (fecha == null) {
            throw new IllegalStateException(
                    "La fecha de reserva es obligatoria."
            );
        }

        if (horaInicio == null || horaFin == null) {
            throw new IllegalStateException(
                    "El horario de la reserva es obligatorio."
            );
        }

        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalStateException(
                    "La hora de finalización debe ser "
                    + "posterior a la hora de inicio."
            );
        }

        if (numeroPersonas < 1) {
            throw new IllegalStateException(
                    "El número de personas debe ser "
                    + "mayor que cero."
            );
        }

        if (piso != null
                && piso.getIdPiso() == null) {

            throw new IllegalStateException(
                    "El piso seleccionado no es válido."
            );
        }
    }
}