package com.rinconcito.repository;

import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Integer> {

    @EntityGraph(attributePaths = {
            "producto"
    })
    List<DetallePedido> findByPedido(
            Pedido pedido
    );

    @EntityGraph(attributePaths = {
            "producto"
    })
    List<DetallePedido> findByPedidoIn(
            List<Pedido> pedidos
    );
}