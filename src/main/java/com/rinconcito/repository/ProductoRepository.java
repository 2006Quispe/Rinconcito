package com.rinconcito.repository;

import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByEstado(EstadoProducto estado);

    List<Producto> findByCategoriaAndEstado(Categoria categoria, EstadoProducto estado);

    List<Producto> findByEstadoOrderByNombreAsc(EstadoProducto estado);

    List<Producto> findAllByOrderByNombreAsc();

    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria c
            WHERE p.estado = :estadoProducto
              AND c.estado = :estadoCategoria
              AND (:categoria IS NULL OR p.categoria = :categoria)
              AND (:q IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY c.nombre ASC, p.nombre ASC
            """)
    List<Producto> buscarPublicos(
            @Param("estadoProducto") EstadoProducto estadoProducto,
            @Param("estadoCategoria") EstadoCategoria estadoCategoria,
            @Param("categoria") Categoria categoria,
            @Param("q") String q);
}
