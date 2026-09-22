package com.rinconcito.repository;

import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.EstadoPedido;
import com.rinconcito.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByCliente(Usuario cliente);
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findByMesero(Usuario mesero);
}