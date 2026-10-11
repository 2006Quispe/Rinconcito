package com.rinconcito.repository;

import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository
        extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByPedido(
            Pedido pedido
    );

    Optional<Pago> findByReserva(
            Reserva reserva
    );
    
    boolean existsByNumeroOperacion(
            String numeroOperacion
    );

    List<Pago> findByEstado(
            EstadoPago estado
    );

    List<Pago>
    findByEstadoAndReservaIsNotNullOrderByFechaPagoAsc(
            EstadoPago estado
    );

    List<Pago>
    findByEstadoAndPedidoIsNotNullOrderByFechaPagoAsc(
            EstadoPago estado
    );

    Optional<Pago>
    findFirstByReservaOrderByFechaPagoDesc(
            Reserva reserva
    );

    Optional<Pago>
    findFirstByReservaAndEstadoOrderByFechaPagoDesc(
            Reserva reserva,
            EstadoPago estado
    );

    List<Pago> findByReservaOrderByFechaPagoDesc(
            Reserva reserva
    );

    Optional<Pago>
    findFirstByPedidoOrderByFechaPagoDesc(
            Pedido pedido
    );

    Optional<Pago>
    findFirstByPedidoAndEstadoOrderByFechaPagoDesc(
            Pedido pedido,
            EstadoPago estado
    );

    List<Pago> findByPedidoOrderByFechaPagoDesc(
            Pedido pedido
    );

    long countByEstadoAndPedidoIsNotNull(
            EstadoPago estado
    );

    long countByEstadoAndReservaIsNotNull(
            EstadoPago estado
    );

    @Query("""
            SELECT COUNT(p)
            FROM Pago p
            WHERE p.estado = :estado
              AND p.fechaValidacion >= :inicio
              AND p.fechaValidacion < :fin
            """)
    long contarValidadosEnFecha(
            @Param("estado")
            EstadoPago estado,

            @Param("inicio")
            LocalDateTime inicio,

            @Param("fin")
            LocalDateTime fin
    );

    /*
     * Obtiene las operaciones del periodo.
     *
     * Si el pago ya fue revisado, utiliza su fecha de
     * validacion. Si sigue pendiente, utiliza la fecha
     * en la que se envio el pago.
     */
    @EntityGraph(attributePaths = {
            "pedido",
            "pedido.cliente",
            "pedido.direccion",
            "reserva",
            "reserva.cliente",
            "reserva.evento",
            "reserva.pisoSolicitado",
            "reserva.mesa",
            "usuarioValidador"
    })
    @Query("""
            SELECT DISTINCT pago
            FROM Pago pago
            WHERE (
                pago.fechaValidacion IS NOT NULL
                AND pago.fechaValidacion >= :desde
                AND pago.fechaValidacion < :hasta
            )
            OR (
                pago.fechaValidacion IS NULL
                AND pago.fechaPago >= :desde
                AND pago.fechaPago < :hasta
            )
            ORDER BY COALESCE(
                pago.fechaValidacion,
                pago.fechaPago
            ) DESC
            """)
    List<Pago> listarOperacionesParaReporte(
            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta
    );
}