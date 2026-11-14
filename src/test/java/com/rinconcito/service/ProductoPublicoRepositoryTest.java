package com.rinconcito.service;

import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.repository.ProductoRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import static org.junit.jupiter.api.Assertions.*;

class ProductoPublicoRepositoryTest {
    @Test
    void detalleConsultaLaBaseYExcluyeProductosOCategoriasInactivas() {
        try (var fabrica = new Configuration()
                .addAnnotatedClass(Producto.class)
                .addAnnotatedClass(Categoria.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:carta_publica;MODE=MySQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
             var sesion = fabrica.openSession()) {
            sesion.beginTransaction();
            Categoria categoria = new Categoria();
            categoria.setNombre("Especialidades");
            sesion.persist(categoria);
            Producto producto = new Producto();
            producto.setCategoria(categoria);
            producto.setNombre("Pachamanca");
            producto.setPrecio(75);
            producto.setIngredientes("Pollo\nPapas nativas");
            sesion.persist(producto);
            sesion.flush();
            Integer id = producto.getIdProducto();
            Integer idCategoria = categoria.getIdCategoria();
            sesion.clear();

            ProductoRepository repositorio = new JpaRepositoryFactory(sesion).getRepository(ProductoRepository.class);
            Producto visible = repositorio.buscarPublicoPorId(id,
                    Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO).orElseThrow();
            assertEquals("Especialidades", visible.getCategoria().getNombre());
            assertEquals("Pollo\nPapas nativas", visible.getIngredientes());

            visible.setDisponible(false);
            sesion.flush();
            assertTrue(repositorio.buscarPublicoPorId(id,
                    Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO).isPresent());

            visible.setEstado(Producto.EstadoProducto.INACTIVO);
            sesion.flush();
            assertTrue(repositorio.buscarPublicoPorId(id,
                    Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO).isEmpty());

            visible.setEstado(Producto.EstadoProducto.ACTIVO);
            sesion.find(Categoria.class, idCategoria).setEstado(Categoria.EstadoCategoria.INACTIVO);
            sesion.flush();
            assertTrue(repositorio.buscarPublicoPorId(id,
                    Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO).isEmpty());
            assertTrue(repositorio.buscarPublicoPorId(-1,
                    Producto.EstadoProducto.ACTIVO, Categoria.EstadoCategoria.ACTIVO).isEmpty());
            sesion.getTransaction().rollback();
        }
    }
}
