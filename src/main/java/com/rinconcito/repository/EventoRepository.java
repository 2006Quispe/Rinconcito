package com.rinconcito.repository;

import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Evento.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByEstadoOrderByFechaAsc(EstadoEvento estado);
    List<Evento> findByFechaAndEstado(java.time.LocalDate fecha, Evento.EstadoEvento estado);
}