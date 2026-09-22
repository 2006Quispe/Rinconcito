package com.rinconcito.repository;

import com.rinconcito.entity.Comanda;
import com.rinconcito.entity.Comanda.EstadoComanda;
import com.rinconcito.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Integer> {
    Optional<Comanda> findByPedido(Pedido pedido);
    List<Comanda> findByEstado(EstadoComanda estado);
}