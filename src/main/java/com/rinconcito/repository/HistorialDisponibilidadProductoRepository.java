package com.rinconcito.repository;

import com.rinconcito.entity.HistorialDisponibilidadProducto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialDisponibilidadProductoRepository
        extends JpaRepository<
                HistorialDisponibilidadProducto,
                Long
        > {

    @EntityGraph(attributePaths = {
            "producto",
            "producto.categoria",
            "usuario",
            "usuario.rol"
    })
    List<HistorialDisponibilidadProducto>
    findTop200ByOrderByFechaCambioDesc();
}
