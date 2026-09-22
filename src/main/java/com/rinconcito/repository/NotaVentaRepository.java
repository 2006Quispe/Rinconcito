package com.rinconcito.repository;

import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.NotaVenta.EstadoNotaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotaVentaRepository extends JpaRepository<NotaVenta, Integer> {
    Optional<NotaVenta> findByNumero(Integer numero);
    List<NotaVenta> findByEstado(EstadoNotaVenta estado);
    List<NotaVenta> findByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta);
}