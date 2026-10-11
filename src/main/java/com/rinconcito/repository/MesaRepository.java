package com.rinconcito.repository;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Mesa.EstadoMesa;
import com.rinconcito.entity.Piso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MesaRepository
        extends JpaRepository<Mesa, Integer> {

    List<Mesa> findByPiso(
            Piso piso
    );

    List<Mesa> findByEstado(
            EstadoMesa estado
    );

    List<Mesa> findByPisoAndEstadoNot(
            Piso piso,
            EstadoMesa estado
    );

    List<Mesa> findByPisoOrderByNumeroAsc(
            Piso piso
    );

    List<Mesa> findByPisoAndEstadoNotOrderByNumeroAsc(
            Piso piso,
            EstadoMesa estado
    );

    @EntityGraph(attributePaths = "piso")
    List<Mesa> findAllByOrderByPisoIdPisoAscNumeroAsc();

    boolean existsByPisoAndNumero(
            Piso piso,
            Integer numero
    );

    boolean existsByPisoAndNumeroAndIdMesaNot(
            Piso piso,
            Integer numero,
            Integer idMesa
    );

    long countByPiso(
            Piso piso
    );

    long countByEstado(
            EstadoMesa estado
    );

    long countByPisoAndEstado(
            Piso piso,
            EstadoMesa estado
    );
}