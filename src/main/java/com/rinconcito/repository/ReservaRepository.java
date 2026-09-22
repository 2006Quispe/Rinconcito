package com.rinconcito.repository;

import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByCliente(Usuario cliente);
    List<Reserva> findByFechaReserva(LocalDate fecha);
    List<Reserva> findByMesaAndFechaReserva(Mesa mesa, LocalDate fecha);
    List<Reserva> findByClienteAndFechaReserva(Usuario cliente, LocalDate fecha);
}