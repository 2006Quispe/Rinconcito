package com.rinconcito.service;

import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import com.rinconcito.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(
            EventoRepository eventoRepository
    ) {
        this.eventoRepository =
                eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<Evento> listarTodos() {
        return eventoRepository
                .findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<Evento> listarActivos() {
        return eventoRepository
                .findByEstadoOrderByFechaAsc(
                        EstadoEvento.ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public Evento buscarPorId(
            Integer idEvento
    ) {
        return eventoRepository
                .findById(idEvento)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Evento no encontrado."
                        )
                );
    }

    @Transactional(readOnly = true)
    public Optional<Evento> buscarEventoActivo(
            LocalDate fecha
    ) {
        if (fecha == null) {
            return Optional.empty();
        }

        return eventoRepository
                .findFirstByFechaAndEstadoOrderByHoraInicioAsc(
                        fecha,
                        EstadoEvento.ACTIVO
                );
    }

    @Transactional
    public Evento guardar(
            Evento evento
    ) {
        validar(evento);

        limpiarDatos(evento);

        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento cambiarEstado(
            Integer idEvento,
            EstadoEvento estado
    ) {
        if (estado == null) {
            throw new IllegalStateException(
                    "Debes seleccionar un estado."
            );
        }

        Evento evento =
                buscarPorId(idEvento);

        evento.setEstado(estado);

        return eventoRepository.save(evento);
    }

    private void validar(
            Evento evento
    ) {
        if (evento == null) {
            throw new IllegalStateException(
                    "Los datos del evento son obligatorios."
            );
        }

        if (
                evento.getTitulo() == null
                        || evento.getTitulo().isBlank()
        ) {
            throw new IllegalStateException(
                    "El título del evento es obligatorio."
            );
        }

        if (evento.getTitulo().trim().length() > 100) {
            throw new IllegalStateException(
                    "El título no puede superar 100 caracteres."
            );
        }

        if (
                evento.getDescripcion() != null
                        && evento.getDescripcion()
                        .trim()
                        .length() > 500
        ) {
            throw new IllegalStateException(
                    "La descripción no puede superar 500 caracteres."
            );
        }

        if (evento.getFecha() == null) {
            throw new IllegalStateException(
                    "La fecha del evento es obligatoria."
            );
        }

        if (
                evento.getFecha().getDayOfWeek()
                        == DayOfWeek.MONDAY
        ) {
            throw new IllegalStateException(
                    "El restaurante no atiende los lunes."
            );
        }

        if (evento.getHoraInicio() == null) {
            throw new IllegalStateException(
                    "La hora de inicio es obligatoria."
            );
        }

        if (
                evento.getHoraFin() != null
                        && !evento.getHoraFin()
                        .isAfter(evento.getHoraInicio())
        ) {
            throw new IllegalStateException(
                    "La hora de finalización debe ser "
                            + "posterior a la hora de inicio."
            );
        }

        if (evento.isRequierePago()) {

            DayOfWeek dia =
                    evento.getFecha().getDayOfWeek();

            if (
                    dia != DayOfWeek.SATURDAY
                            && dia != DayOfWeek.SUNDAY
            ) {
                throw new IllegalStateException(
                        "Solo los eventos de sábado o domingo "
                                + "pueden cobrar entrada."
                );
            }

            if (
                    evento.getPrecioEntrada() == null
                            || evento.getPrecioEntrada() <= 0
            ) {
                throw new IllegalStateException(
                        "Debes ingresar un precio de entrada "
                                + "mayor que cero."
                );
            }
        }

        if (evento.getEstado() == null) {
            evento.setEstado(
                    EstadoEvento.ACTIVO
            );
        }
    }

    private void limpiarDatos(
            Evento evento
    ) {
        evento.setTitulo(
                evento.getTitulo().trim()
        );

        if (evento.getDescripcion() != null) {
            String descripcion =
                    evento.getDescripcion().trim();

            evento.setDescripcion(
                    descripcion.isBlank()
                            ? null
                            : descripcion
            );
        }

        if (evento.getImagen() != null) {
            String imagen =
                    evento.getImagen().trim();

            evento.setImagen(
                    imagen.isBlank()
                            ? null
                            : imagen
            );
        }

        if (!evento.isRequierePago()) {
            evento.setPrecioEntrada(0);
        }
    }
}