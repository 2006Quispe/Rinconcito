package com.rinconcito.repository;

import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Piso.EstadoPiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PisoRepository
        extends JpaRepository<Piso, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Piso p where p.idPiso = :id")
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