package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Categoria.EstadoCategoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Producto.EstadoProducto;
import com.rinconcito.repository.CategoriaRepository;
import com.rinconcito.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private static final int MAX_DESCRIPCION = 300;
    private static final int MAX_NOMBRE = 100;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoImagenStorageService
            productoImagenStorageService;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            ProductoImagenStorageService
                    productoImagenStorageService
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoImagenStorageService =
                productoImagenStorageService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(
            Categoria categoria
    ) {
        return productoRepository
                .findByCategoriaAndEstado(
                        categoria,
                        EstadoProducto.ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public List<Producto> listarActivos() {
        return productoRepository
                .findByEstadoOrderByNombreAsc(
                        EstadoProducto.ACTIVO
                );
    }

    @Transactional(readOnly = true)
    public List<Producto> listarTodosAdmin() {
        return productoRepository
                .findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Integer id) {
        return productoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPublicos(
            String q,
            Integer idCategoria
    ) {
        String busqueda =
                q == null || q.isBlank()
                        ? null
                        : q.trim();

        Categoria categoria = null;

        if (idCategoria != null) {
            categoria = categoriaRepository
                    .findById(idCategoria)
                    .orElse(null);

            if (categoria == null) {
                return List.of();
            }
        }

        return productoRepository.buscarPublicos(
                EstadoProducto.ACTIVO,
                EstadoCategoria.ACTIVO,
                categoria,
                busqueda
        );
    }

    @Transactional
    public Producto guardar(
            Producto producto,
            Integer idCategoria,
            MultipartFile archivoImagen,
            boolean eliminarImagen
    ) {
        validar(producto, idCategoria);

        Categoria categoria =
                categoriaRepository
                        .findById(idCategoria)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "La categoría indicada no existe."
                                )
                        );

        String imagenAnterior =
                producto.getImagen();

        String imagenNueva = null;

        boolean tieneNuevaImagen =
                archivoImagen != null
                        && !archivoImagen.isEmpty();

        if (tieneNuevaImagen) {
            imagenNueva =
                    productoImagenStorageService
                            .guardar(archivoImagen);

            producto.setImagen(imagenNueva);

        } else if (eliminarImagen) {
            producto.setImagen(null);
        }

        producto.setCategoria(categoria);

        if (producto.getEstado() == null) {
            producto.setEstado(
                    EstadoProducto.ACTIVO
            );
        }

        if (producto.getDescripcion() != null) {
            String descripcion =
                    producto.getDescripcion().trim();

            producto.setDescripcion(
                    descripcion.isBlank()
                            ? null
                            : descripcion
            );
        }

        try {
            Producto guardado =
                    productoRepository.save(producto);

            boolean cambioImagen =
                    tieneNuevaImagen
                            || eliminarImagen;

            if (cambioImagen
                    && imagenAnterior != null
                    && !imagenAnterior.isBlank()
                    && !imagenAnterior.equals(
                            producto.getImagen()
                    )) {
                productoImagenStorageService
                        .eliminar(imagenAnterior);
            }

            return guardado;

        } catch (RuntimeException e) {
            if (imagenNueva != null) {
                productoImagenStorageService
                        .eliminar(imagenNueva);
            }

            throw e;
        }
    }

    @Transactional
    public void cambiarDisponibilidad(
            Integer idProducto,
            boolean disponible
    ) {
        Producto producto =
                productoRepository
                        .findById(idProducto)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Producto no encontrado."
                                )
                        );

        producto.setDisponible(disponible);
        productoRepository.save(producto);
    }

    @Transactional
    public void cambiarEstado(
            Integer idProducto,
            EstadoProducto estado
    ) {
        if (estado == null) {
            throw new IllegalStateException(
                    "El estado es obligatorio."
            );
        }

        Producto producto =
                productoRepository
                        .findById(idProducto)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Producto no encontrado."
                                )
                        );

        producto.setEstado(estado);
        productoRepository.save(producto);
    }

    private void validar(
            Producto producto,
            Integer idCategoria
    ) {
        if (producto == null) {
            throw new IllegalStateException(
                    "Los datos del producto son obligatorios."
            );
        }

        if (producto.getNombre() == null
                || producto.getNombre().isBlank()) {
            throw new IllegalStateException(
                    "El nombre del producto es obligatorio."
            );
        }

        String nombre =
                producto.getNombre().trim();

        if (nombre.length() > MAX_NOMBRE) {
            throw new IllegalStateException(
                    "El nombre no puede superar "
                            + MAX_NOMBRE
                            + " caracteres."
            );
        }

        producto.setNombre(nombre);

        if (producto.getPrecio() == null
                || producto.getPrecio() <= 0) {
            throw new IllegalStateException(
                    "El precio debe ser un entero mayor a 0."
            );
        }

        if (idCategoria == null) {
            throw new IllegalStateException(
                    "Debe seleccionar una categoría."
            );
        }

        if (producto.getDescripcion() != null
                && producto.getDescripcion().trim().length()
                > MAX_DESCRIPCION) {
            throw new IllegalStateException(
                    "La descripción no puede superar "
                            + MAX_DESCRIPCION
                            + " caracteres."
            );
        }
    }
}