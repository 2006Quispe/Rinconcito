package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import com.rinconcito.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private static final int MAX_NOMBRE = 60;
    private static final int MAX_DESCRIPCION = 200;

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> listarActivas() {
        return categoriaRepository.findByEstadoOrderByNombreAsc(EstadoCategoria.ACTIVO);
    }

    public List<Categoria> listarTodasAdmin() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    public Optional<Categoria> buscarPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    @Transactional
    public Categoria guardar(Categoria categoria) {
        validar(categoria);
        if (categoria.getEstado() == null) {
            categoria.setEstado(EstadoCategoria.ACTIVO);
        }
        if (categoria.getDescripcion() != null && categoria.getDescripcion().isBlank()) {
            categoria.setDescripcion(null);
        }
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void cambiarEstado(Integer idCategoria, EstadoCategoria estado) {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));
        categoria.setEstado(estado);
        categoriaRepository.save(categoria);
    }

    private void validar(Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new IllegalStateException("El nombre de la categoría es obligatorio.");
        }
        String nombre = categoria.getNombre().trim();
        if (nombre.length() > MAX_NOMBRE) {
            throw new IllegalStateException("El nombre no puede superar " + MAX_NOMBRE + " caracteres.");
        }
        categoria.setNombre(nombre);

        boolean duplicado;
        if (categoria.getIdCategoria() == null) {
            duplicado = categoriaRepository.existsByNombreIgnoreCase(nombre);
        } else {
            duplicado = categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, categoria.getIdCategoria());
        }
        if (duplicado) {
            throw new IllegalStateException("Ya existe una categoría con ese nombre.");
        }

        if (categoria.getDescripcion() != null && categoria.getDescripcion().length() > MAX_DESCRIPCION) {
            throw new IllegalStateException("La descripción no puede superar " + MAX_DESCRIPCION + " caracteres.");
        }
    }
}
