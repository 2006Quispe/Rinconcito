package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoIngredientesTest {
    private ProductoRepository productos;
    private CategoriaRepository categorias;
    private ProductoService servicio;
    private Producto producto;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoRepository.class);
        categorias = mock(CategoriaRepository.class);
        servicio = new ProductoService(productos, categorias, mock(UsuarioRepository.class),
                mock(HistorialDisponibilidadProductoRepository.class), mock(ProductoImagenStorageService.class));
        producto = new Producto();
        producto.setNombre("Pachamanca");
        producto.setPrecio(75);
        when(categorias.findById(1)).thenReturn(Optional.of(new Categoria()));
        when(productos.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void separaIngredientesPorComasYLineasSinVaciosNiDuplicados() {
        producto.setIngredientes(" Pollo, Papas nativas\r\nHabas, , Pollo\n ");
        assertEquals(List.of("Pollo", "Papas nativas", "Habas"), producto.getListaIngredientes());
        servicio.guardar(producto, 1, null, false);
        assertEquals("Pollo\nPapas nativas\nHabas", producto.getIngredientes());
    }

    @Test
    void ingredientesSonOpcionalesYSePuedenQuitarAlEditar() {
        assertTrue(producto.getListaIngredientes().isEmpty());
        servicio.guardar(producto, 1, null, false);
        assertNull(producto.getIngredientes());
        producto.setIngredientes(" , \n ");
        servicio.guardar(producto, 1, null, false);
        assertNull(producto.getIngredientes());
    }

    @Test
    void validaElLimiteAntesDeGuardar() {
        producto.setIngredientes("a".repeat(1001));
        assertThrows(IllegalStateException.class, () -> servicio.guardar(producto, 1, null, false));
        verify(productos, never()).save(any());
        producto.setIngredientes("a".repeat(1000));
        assertDoesNotThrow(() -> servicio.guardar(producto, 1, null, false));
    }

    @Test
    void consultaDetalleExigeProductoYCategoriaActivos() {
        when(productos.buscarPublicoPorId(7, Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO))
                .thenReturn(Optional.of(producto));
        assertSame(producto, servicio.buscarPublicoPorId(7).orElseThrow());
        verify(productos).buscarPublicoPorId(7, Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO);
    }
}
