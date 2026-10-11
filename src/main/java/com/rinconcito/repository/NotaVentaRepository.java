package com.rinconcito.repository;

import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.NotaVenta.EstadoNotaVenta;
import com.rinconcito.entity.Pago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotaVentaRepository
        extends JpaRepository<NotaVenta, Integer> {

    Optional<NotaVenta> findByNumero(
            Integer numero
    );

    Optional<NotaVenta> findBySerieAndNumero(
            String serie,
            Integer numero
    );

    Optional<NotaVenta> findByPago(
            Pago pago
    );

    boolean existsByPago(
            Pago pago
    );

    Optional<NotaVenta>
    findFirstBySerieOrderByNumeroDesc(
            String serie
    );

    List<NotaVenta> findByEstado(
            EstadoNotaVenta estado
    );

    List<NotaVenta> findByFechaEmisionBetween(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    long countByEstadoAndFechaEmisionBetween(
            EstadoNotaVenta estado,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @Query("""
            SELECT COALESCE(SUM(nota.total), 0)
            FROM NotaVenta nota
            WHERE nota.estado = :estado
              AND nota.fechaEmision >= :desde
              AND nota.fechaEmision < :hasta
            """)
    Long sumarVentasPorPeriodo(
            @Param("estado")
            EstadoNotaVenta estado,

            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta
    );

    @Query("""
            SELECT nota.pago.metodoPago,
                   COUNT(nota),
                   COALESCE(SUM(nota.total), 0)
            FROM NotaVenta nota
            WHERE nota.estado = :estado
              AND nota.fechaEmision >= :desde
              AND nota.fechaEmision < :hasta
            GROUP BY nota.pago.metodoPago
            ORDER BY COUNT(nota) DESC
            """)
    List<Object[]> resumirPorMetodoPago(
            @Param("estado")
            EstadoNotaVenta estado,

            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta
    );

    /*
     * Historial completo de Caja:
     * incluye notas de pedidos y reservas.
     */
    @EntityGraph(attributePaths = {
            "pago",
            "usuarioEmisor",
            "pago.pedido",
            "pago.pedido.cliente",
            "pago.reserva",
            "pago.reserva.cliente",
            "pago.reserva.evento",
            "pago.reserva.pisoSolicitado",
            "pago.reserva.mesa"
    })
    @Query("""
            SELECT nota
            FROM NotaVenta nota
            ORDER BY nota.fechaEmision DESC
            """)
    List<NotaVenta> listarNotasParaCaja();

    /*
     * Últimas notas para el dashboard.
     */
    @EntityGraph(attributePaths = {
            "pago",
            "usuarioEmisor",
            "pago.pedido",
            "pago.pedido.cliente",
            "pago.reserva",
            "pago.reserva.cliente"
    })
    List<NotaVenta>
    findTop5ByOrderByFechaEmisionDesc();

    /*
     * Obtiene una nota con todos los datos
     * necesarios para generar el PDF.
     */
    @EntityGraph(attributePaths = {
            "pago",
            "usuarioEmisor",
            "pago.pedido",
            "pago.pedido.cliente",
            "pago.pedido.direccion",
            "pago.reserva",
            "pago.reserva.cliente",
            "pago.reserva.evento",
            "pago.reserva.pisoSolicitado",
            "pago.reserva.mesa"
    })
    @Query("""
            SELECT nota
            FROM NotaVenta nota
            WHERE nota.idNota = :idNota
            """)
    Optional<NotaVenta> buscarPorIdConDatos(
            @Param("idNota")
            Integer idNota
    );
}