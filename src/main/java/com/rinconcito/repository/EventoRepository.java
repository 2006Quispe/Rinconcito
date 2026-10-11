package com.rinconcito.repository;

import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoRepository
        extends JpaRepository<Evento, Integer> {

    List<Evento> findAllByOrderByFechaDesc();

    List<Evento> findByEstadoOrderByFechaAsc(
            EstadoEvento estado
    );

    List<Evento> findByFechaAndEstado(
            LocalDate fecha,
            EstadoEvento estado
    );

    Optional<Evento>
    findFirstByFechaAndEstadoOrderByHoraInicioAsc(
            LocalDate fecha,
            EstadoEvento estado
    );
}