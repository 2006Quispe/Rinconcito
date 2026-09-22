package com.rinconcito.repository;

import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    Optional<Pago> findByPedido(Pedido pedido);
    Optional<Pago> findByReserva(Reserva reserva);
    List<Pago> findByEstado(EstadoPago estado);
}