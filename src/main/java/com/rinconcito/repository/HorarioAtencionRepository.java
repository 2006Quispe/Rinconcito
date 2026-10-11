package com.rinconcito.repository;

import com.rinconcito.entity.HorarioAtencion;
import com.rinconcito.entity.HorarioAtencion.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Integer> {
    Optional<HorarioAtencion> findByDiaSemana(DiaSemana diaSemana);
}
