package com.rinconcito.repository;

import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByEstado(EstadoProducto estado);
    List<Producto> findByCategoriaAndEstado(Categoria categoria, EstadoProducto estado);
}