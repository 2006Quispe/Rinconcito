package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoRecomendacionesTest {
    private ProductoRepository repositorio;
    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ProductoRepository.class);
        servicio = new ProductoService(repositorio, mock(CategoriaRepository.class),
                mock(UsuarioRepository.class), mock(HistorialDisponibilidadProductoRepository.class),
                mock(ProductoImagenStorageService.class));
    }

    private Producto producto(int id, int idCategoria, boolean disponible) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(idCategoria);
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setCategoria(categoria);
        producto.setDisponible(disponible);
        return producto;
    }

    private void catalogo(List<Producto> productos) {
        when(repositorio.buscarPublicos(Producto.EstadoProducto.ACTIVO,
                Categoria.EstadoCategoria.ACTIVO, null, null)).thenReturn(productos);
    }

    @Test
    void incluyeOtrasCategoriasSinElProductoActualNiAgotados() {
        Producto otro = producto(2, 2, true);
        catalogo(List.of(producto(1, 1, true), otro, producto(3, 3, false)));
        assertEquals(List.of(otro), servicio.recomendarPublicos(1));
        verify(repositorio).buscarPublicos(Producto.EstadoProducto.ACTIVO,
                Categoria.EstadoCategoria.ACTIVO, null, null);
    }

    @Test
    void limitaATresSinRepetirNiModificarLaListaOriginal() {
        List<Producto> disponibles = List.of(producto(2, 1, true), producto(3, 2, true),
                producto(4, 3, true), producto(5, 2, true), producto(6, 1, true));
        catalogo(disponibles);
        for (int intento = 0; intento < 20; intento++) {
            List<Producto> resultado = servicio.recomendarPublicos(1);
            assertEquals(3, resultado.size());
            assertEquals(3, resultado.stream().map(Producto::getIdProducto).distinct().count());
            assertTrue(disponibles.containsAll(resultado));
        }
        assertEquals(List.of(2, 3, 4, 5, 6), disponibles.stream().map(Producto::getIdProducto).toList());
    }

    @Test
    void devuelveLasOpcionesExistentesSiHayMenosDeTres() {
        catalogo(List.of(producto(2, 2, true), producto(3, 3, true)));
        assertEquals(2, servicio.recomendarPublicos(1).size());
    }

    @Test
    void noInventaProductosCuandoNoHayCandidatos() {
        catalogo(List.of(producto(1, 1, true), producto(2, 2, false)));
        assertTrue(servicio.recomendarPublicos(1).isEmpty());
        catalogo(List.of());
        assertTrue(servicio.recomendarPublicos(1).isEmpty());
    }
}
