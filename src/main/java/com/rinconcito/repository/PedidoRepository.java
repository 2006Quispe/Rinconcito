package com.rinconcito.repository;

import com.rinconcito.entity.Mesa;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository
        extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByCliente(
            Usuario cliente
    );

    /*Pedidos del cliente ordenados desde el más reciente.*/
    @EntityGraph(attributePaths = {
            "direccion",
            "mesa"
    })
    List<Pedido> findByClienteOrderByFechaRegistroDesc(
            Usuario cliente
    );

    List<Pedido> findByEstado(
            EstadoPedido estado
    );

    List<Pedido> findByMesero(
            Usuario mesero
    );

    Optional<Pedido> findByCodigoPedido(
            String codigoPedido
    );

    Optional<Pedido>
    findTopByCodigoPedidoStartingWithOrderByCodigoPedidoDesc(
            String prefix
    );

    /*
     * Consulta utilizada por el administrador
     * para filtrar pedidos.
     */
    @EntityGraph(attributePaths = {
            "cliente",
            "mesero",
            "mesa",
            "mesa.piso",
            "direccion"
    })
    @Query("""
            SELECT pedido
            FROM Pedido pedido
            WHERE (
                :desde IS NULL
                OR pedido.fechaRegistro >= :desde
            )
            AND (
                :hasta IS NULL
                OR pedido.fechaRegistro < :hasta
            )
            AND (
                :estado IS NULL
                OR pedido.estado = :estado
            )
            AND (
                :tipoPedido IS NULL
                OR pedido.tipoPedido = :tipoPedido
            )
            AND (
                :busqueda IS NULL
                OR LOWER(pedido.codigoPedido)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(pedido.cliente.nombres)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(pedido.cliente.apellidos)
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR LOWER(
                    CONCAT(
                        pedido.cliente.nombres,
                        ' ',
                        pedido.cliente.apellidos
                    )
                )
                    LIKE LOWER(
                        CONCAT('%', :busqueda, '%')
                    )
                OR pedido.cliente.dni
                    LIKE CONCAT('%', :busqueda, '%')
            )
            ORDER BY pedido.fechaRegistro DESC,
                     pedido.idPedido DESC
            """)
    List<Pedido> buscarParaAdministracion(
            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta,

            @Param("estado")
            EstadoPedido estado,

            @Param("tipoPedido")
            TipoPedido tipoPedido,

            @Param("busqueda")
            String busqueda
    );

    /*
     * Comprueba si una mesa ya tiene un pedido
     * presencial que todavía no ha finalizado.
     */
    boolean existsByMesaAndTipoPedidoAndEstadoIn(
            Mesa mesa,
            TipoPedido tipoPedido,
            List<EstadoPedido> estados
    );

    /*
     * Lista los pedidos presenciales activos
     * para el panel del mesero.
     */
    @EntityGraph(attributePaths = {
            "mesa",
            "mesa.piso",
            "mesero",
            "cliente"
    })
    List<Pedido>
    findByTipoPedidoAndEstadoInOrderByFechaRegistroAsc(
            TipoPedido tipoPedido,
            List<EstadoPedido> estados
    );

    /*
     * Obtiene un pedido presencial con sus
     * asociaciones principales cargadas.
     */
    @EntityGraph(attributePaths = {
            "mesa",
            "mesa.piso",
            "mesero",
            "cliente"
    })
    Optional<Pedido>
    findByIdPedidoAndTipoPedido(
            Integer idPedido,
            TipoPedido tipoPedido
    );
}