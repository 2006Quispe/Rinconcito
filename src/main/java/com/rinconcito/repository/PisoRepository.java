package com.rinconcito.repository;

import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PisoRepository
        extends JpaRepository<Piso, Integer> {

    List<Piso> findByEstado(
            EstadoPiso estado
    );

    List<Piso> findAllByOrderByIdPisoAsc();

    List<Piso> findByEstadoOrderByIdPisoAsc(
            EstadoPiso estado
    );

    boolean existsByNombreIgnoreCase(
            String nombre
    );

    boolean existsByNombreIgnoreCaseAndIdPisoNot(
            String nombre,
            Integer idPiso
    );

    long countByEstado(
            EstadoPiso estado
    );
}