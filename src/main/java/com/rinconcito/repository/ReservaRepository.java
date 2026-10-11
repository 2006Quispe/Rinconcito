package com.rinconcito.repository;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository
        extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByCliente(
            Usuario cliente
    );

    @EntityGraph(attributePaths = {
            "evento",
            "pisoSolicitado",
            "mesa"
    })
    List<Reserva> findByClienteOrderByFechaRegistroDesc(
            Usuario cliente
    );

    List<Reserva> findByFechaReserva(
            LocalDate fecha
    );

    List<Reserva> findByMesaAndFechaReserva(
            Mesa mesa,
            LocalDate fecha
    );

    List<Reserva> findByClienteAndFechaReserva(
            Usuario cliente,
            LocalDate fecha
    );

    Optional<Reserva>
    findTopByCodigoReservaStartingWithOrderByCodigoReservaDesc(
            String prefix
    );

    Optional<Reserva> findByCodigoReserva(
            String codigoReserva
    );

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE (:fecha IS NULL OR r.fechaReserva = :fecha)
              AND (:piso IS NULL OR r.pisoSolicitado = :piso)
              AND (:estado IS NULL OR r.estado = :estado)
            ORDER BY
                r.fechaReserva DESC,
                r.horaInicio DESC,
                r.idReserva DESC
            """)
    List<Reserva> buscarConFiltros(
            @Param("fecha")
            LocalDate fecha,

            @Param("piso")
            Piso piso,

            @Param("estado")
            EstadoReserva estado
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "mesa",
            "mesa.piso",
            "pisoSolicitado",
            "evento"
    })
    @Query("""
            SELECT reserva
            FROM Reserva reserva
            WHERE reserva.fechaReserva = :fecha
              AND reserva.mesa IS NOT NULL
              AND reserva.estado IN :estados
            ORDER BY
                reserva.horaInicio ASC,
                reserva.mesa.numero ASC
            """)
    List<Reserva> listarReservasOperativasDelDia(
            @Param("fecha")
            LocalDate fecha,

            @Param("estados")
            List<EstadoReserva> estados
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "mesa",
            "mesa.piso",
            "pisoSolicitado",
            "evento"
    })
    @Query("""
            SELECT reserva
            FROM Reserva reserva
            WHERE reserva.idReserva = :idReserva
            """)
    Optional<Reserva> buscarPorIdParaMesero(
            @Param("idReserva")
            Integer idReserva
    );
}