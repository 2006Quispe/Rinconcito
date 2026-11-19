package com.rinconcito.repository;

import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PisoRepository
        extends JpaRepository<Piso, Integer> {

    @Query(value = "SELECT * FROM pisos WHERE id_piso = :id FOR UPDATE", nativeQuery = true)
    Optional<Piso> bloquearPorId(@Param("id") Integer id);

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