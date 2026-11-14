package com.rinconcito.controller;

import com.rinconcito.dto.CarritoItemDTO;
import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CarritoService;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartaControllerTest {
    private ProductoService productos;
    private CategoriaService categorias;
    private CarritoService carrito;
    private MockMvc web;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoService.class);
        categorias = mock(CategoriaService.class);
        carrito = mock(CarritoService.class);
        CartaController carta = new CartaController();
        ReflectionTestUtils.setField(carta, "productoService", productos);
        ReflectionTestUtils.setField(carta, "categoriaService", categorias);
        ReflectionTestUtils.setField(carta, "carritoService", carrito);
        web = MockMvcBuilders.standaloneSetup(carta, new CarritoController(carrito)).build();
        categoria = new Categoria();
        categoria.setIdCategoria(1);
        categoria.setNombre("Platos de fondo");
    }

    private Producto producto(int id, boolean disponible) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setCategoria(categoria);
        producto.setNombre("Plato " + id);
        producto.setPrecio(75);
        producto.setDisponible(disponible);
        return producto;
    }

    @Test
    void conservaLosFiltrosYElResumenDeLaSesion() throws Exception {
        when(categorias.listarActivas()).thenReturn(List.of(categoria));
        when(productos.buscarPublicos("pacha", 1)).thenReturn(List.of(producto(1, true)));
        when(carrito.obtenerItems(any())).thenReturn(List.of());
        web.perform(get("/carta").param("q", "pacha").param("categoria", "1"))
                .andExpect(status().isOk()).andExpect(view().name("paginas/carta"))
                .andExpect(model().attribute("q", "pacha"))
                .andExpect(model().attribute("categoriaSeleccionada", 1))
                .andExpect(model().attribute("totalProductos", 1))
                .andExpect(model().attribute("itemsCarrito", List.of()));
    }

    @Test
    void detalleIncluyeRecomendacionesDeOtraCategoriaYConservaFiltros() throws Exception {
        Producto principal = producto(1, true);
        Producto relacionado = producto(2, true);
        Categoria otraCategoria = new Categoria();
        otraCategoria.setIdCategoria(2);
        otraCategoria.setNombre("Bebidas");
        relacionado.setCategoria(otraCategoria);
        when(productos.buscarPublicoPorId(1)).thenReturn(Optional.of(principal));
        when(productos.recomendarPublicos(1)).thenReturn(List.of(relacionado));
        web.perform(get("/carta/1").param("q", "pacha").param("categoria", "1"))
                .andExpect(status().isOk()).andExpect(view().name("paginas/producto-detalle"))
                .andExpect(model().attribute("producto", principal))
                .andExpect(model().attribute("relacionados", List.of(relacionado)))
                .andExpect(model().attribute("q", "pacha"))
                .andExpect(model().attribute("categoriaSeleccionada", 1));
        verify(productos).recomendarPublicos(1);
    }

    @Test
    void unProductoAgotadoConservaSuDetalle() throws Exception {
        Producto agotado = producto(1, false);
        when(productos.buscarPublicoPorId(1)).thenReturn(Optional.of(agotado));
        when(productos.recomendarPublicos(1)).thenReturn(List.of());
        web.perform(get("/carta/1")).andExpect(status().isOk())
                .andExpect(model().attribute("producto", agotado));
    }

    @Test
    void devuelve404CuandoElProductoNoEsPublico() throws Exception {
        when(productos.buscarPublicoPorId(99)).thenReturn(Optional.empty());
        web.perform(get("/carta/99")).andExpect(status().isNotFound());
        verify(productos, never()).buscarPublicos(any(), any());
        verify(productos, never()).recomendarPublicos(any());
    }

    @Test
    void agregarDevuelveResumenYConservaElContratoAnterior() throws Exception {
        when(carrito.contarUnidades(any())).thenReturn(2);
        when(carrito.subtotal(any())).thenReturn(150);
        when(carrito.obtenerItems(any())).thenReturn(List.of(new CarritoItemDTO(1, "Pachamanca", null, 75, 2)));
        web.perform(post("/carrito/agregar-ajax").param("idProducto", "1").param("cantidad", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.cantidadCarrito").value(2))
                .andExpect(jsonPath("$.subtotalCarrito").value(150))
                .andExpect(jsonPath("$.itemsCarrito[0].nombre").value("Pachamanca"));
    }

    @Test
    void agregarRechazadoNoDevuelveExito() throws Exception {
        doThrow(new IllegalStateException("Producto no disponible."))
                .when(carrito).agregar(any(), eq(1), eq(1));
        web.perform(post("/carrito/agregar-ajax").param("idProducto", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false));
    }
}
