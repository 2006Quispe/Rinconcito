package com.rinconcito.repository;

import com.rinconcito.entity.Galeria;
import com.rinconcito.entity.Galeria.EstadoGaleria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GaleriaRepository
        extends JpaRepository<Galeria, Integer> {

    List<Galeria>
    findByEstadoOrderByFechaPublicacionDesc(
            EstadoGaleria estado
    );

    List<Galeria>
    findAllByOrderByFechaPublicacionDesc();
}