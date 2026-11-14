package com.rinconcito.controller;

import com.rinconcito.config.SecurityConfig;
import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.service.CarritoService;
import com.rinconcito.service.CategoriaService;
import com.rinconcito.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(CartaVistaTest.Config.class)
class CartaVistaTest {
    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class Config {
        @Bean com.rinconcito.repository.CategoriaRepository categoriaRepository() {
            return mock(com.rinconcito.repository.CategoriaRepository.class);
        }
        @Bean com.rinconcito.repository.ProductoRepository productoRepository() {
            return mock(com.rinconcito.repository.ProductoRepository.class);
        }
        @Bean ProductoService productoService() { return mock(ProductoService.class); }
        @Bean CategoriaService categoriaService() { return mock(CategoriaService.class); }
        @Bean CarritoService carritoService() { return mock(CarritoService.class); }
        @Bean UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
        @Bean CartaController cartaController() { return new CartaController(); }
        @Bean SpringResourceTemplateResolver plantillas() {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setPrefix("classpath:/templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
        @Bean SpringTemplateEngine motor(SpringResourceTemplateResolver plantillas) {
            SpringTemplateEngine motor = new SpringTemplateEngine();
            motor.setTemplateResolver(plantillas);
            motor.addDialect(new SpringSecurityDialect());
            return motor;
        }
        @Bean ThymeleafViewResolver vistas(SpringTemplateEngine motor) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(motor);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
    @Autowired WebApplicationContext contexto;
    @Autowired ProductoService productos;
    @Autowired CategoriaService categorias;
    @Autowired CarritoService carrito;
    private MockMvc web;
    private Producto principal;

    @BeforeEach
    void preparar() {
        reset(productos, categorias, carrito);
        web = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(1);
        categoria.setNombre("Especialidades campestres");
        categoria.setDescripcion("Sabores tradicionales para compartir.");
        principal = producto(1, "Pachamanca 3 sabores", "/img/pachamanca.png", categoria);
        principal.setIngredientes("Pollo, Cerdo, Cordero, Papas nativas, Habas");
        principal.setDescripcion("Pachamanca preparada con tres tipos de carne, acompañada de papas y habas. El sabor de nuestra tradición en tu mesa.");
        List<Producto> carta = List.of(principal,
                producto(2, "Chicharrón de cerdo", "/img/chicarron.jpg", categoria),
                producto(3, "Caja china", "/img/caja-china.jpg", categoria),
                producto(4, "Cuy chactado", "/img/cuy-chactado.png", categoria));
        when(categorias.listarActivas()).thenReturn(List.of(categoria));
        when(productos.buscarPublicos(any(), any())).thenReturn(carta);
        when(productos.buscarPublicoPorId(1)).thenReturn(Optional.of(principal));
        when(carrito.obtenerItems(any())).thenReturn(List.of());
    }

    private Producto producto(int id, String nombre, String imagen, Categoria categoria) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setNombre(nombre);
        producto.setImagen(imagen);
        producto.setCategoria(categoria);
        producto.setPrecio(id == 1 ? 75 : 55);
        producto.setDescripcion("Sabor tradicional, preparado para compartir en tu mesa.");
        return producto;
    }

    @Test
    void renderizaCartaYDetallePublicosConCsrfYFiltros() throws Exception {
        String carta = web.perform(get("/carta").param("q", "pacha").param("categoria", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/carta/1?q=pacha&amp;categoria=1")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andReturn().getResponse().getContentAsString();
        String detalle = web.perform(get("/carta/1").param("q", "pacha").param("categoria", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Papas nativas")))
                .andExpect(content().string(containsString("/carta?q=pacha&amp;categoria=1")))
                .andExpect(content().string(not(containsString("Ver en 3D"))))
                .andReturn().getResponse().getContentAsString();
        Path destino = Path.of("target", "carta-vistas-prueba");
        Files.createDirectories(destino);
        Files.writeString(destino.resolve("carta.html"), carta);
        Files.writeString(destino.resolve("detalle.html"), detalle);
    }

    @Test
    void agotadoSinIngredientesNoOfreceComprarNiInventaIngredientes() throws Exception {
        principal.setDisponible(false);
        principal.setIngredientes(null);
        web.perform(get("/carta/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agotado por hoy")))
                .andExpect(content().string(not(containsString("data-agregar-carrito"))))
                .andExpect(content().string(not(containsString("id=\"tituloIngredientes\""))));
    }

    @Test
    void busquedaSinResultadosTieneSalidaALaCarta() throws Exception {
        when(productos.buscarPublicos("inexistente", null)).thenReturn(List.of());
        web.perform(get("/carta").param("q", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No encontramos productos")));
    }
}
