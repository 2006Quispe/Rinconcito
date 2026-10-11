package com.rinconcito.repository;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findByEstado(EstadoCategoria estado);

    List<Categoria> findByEstadoOrderByNombreAsc(EstadoCategoria estado);

    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdCategoriaNot(String nombre, Integer idCategoria);
}
