package com.rinconcito;

import com.rinconcito.entity.Piso;
import com.rinconcito.entity.Categoria;
import com.rinconcito.entity.Producto;
import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Pedido;
import com.rinconcito.repository.CategoriaRepository;
import com.rinconcito.repository.ProductoRepository;
import com.rinconcito.repository.RolRepository;
import com.rinconcito.repository.PedidoRepository;
import com.rinconcito.entity.Evento;
import com.rinconcito.repository.EventoRepository;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.rinconcito.repository.PisoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:arranque;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.correo.habilitado=false"
})
class RinconcitoApplicationTests {

    @Autowired
    private PisoRepository pisos;

    @Autowired
    private EventoRepository eventos;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private RolRepository roles;
    @Autowired private PedidoRepository pedidos;

    @Test
    @Transactional
    void elPedidoSeAbreEnLaPasarelaCompartidaYSeConfirma() throws Exception {
        Rol cliente = new Rol();
        cliente.setNombre("CLIENTE");
        roles.saveAndFlush(cliente);
        Categoria categoria = new Categoria();
        categoria.setNombre("Pruebas de checkout");
        categorias.saveAndFlush(categoria);
        Producto producto = new Producto();
        producto.setNombre("Pachamanca de prueba");
        producto.setPrecio(35);
        producto.setCategoria(categoria);
        producto.setDisponible(true);
        productos.saveAndFlush(producto);

        var web = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        var sesion = new MockHttpSession();
        web.perform(post("/carrito/agregar").session(sesion).with(csrf())
                        .param("idProducto", producto.getIdProducto().toString()).param("cantidad", "1"))
                .andExpect(redirectedUrl("/carrito"));
        web.perform(post("/pedidos").session(sesion).with(csrf())
                        .param("dni", "00000002").param("nombres", "Persona").param("apellidos", "Prueba")
                        .param("telefono", "900000002").param("correo", "persona@example.test")
                        .param("tipoEntrega", "RECOJO_LOCAL"))
                .andExpect(redirectedUrl("/pedidos/pago-demo"));

        Pedido pedido = pedidos.findAll().getFirst();
        String codigo = pedido.getCodigoPedido();
        web.perform(get("/pedidos/pago-demo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagar S/ 35.00")))
                .andExpect(content().string(containsString("Número de tarjeta")));
        web.perform(post("/pedidos/pago-demo").session(sesion).with(csrf())
                        .param("codigo", codigo).param("metodo", "PLIN"))
                .andExpect(redirectedUrl("/pedidos/consultar?codigo=" + codigo));
        assertEquals(Pedido.EstadoPedido.CONFIRMADO, pedidos.findByCodigoPedido(codigo).orElseThrow().getEstado());
        web.perform(get("/pedidos/consultar").param("codigo", codigo).session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void lasReservasConEntradaAbrenLaPasarelaPorDefecto() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Evento evento = new Evento();
        evento.setTitulo("Almuerzo show");
        evento.setFecha(fecha);
        evento.setHoraInicio(LocalTime.of(14, 0));
        evento.setRequierePago(true);
        evento.setPrecioEntrada(20);
        eventos.saveAndFlush(evento);

        var web = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        var sesion = new MockHttpSession();
        var envio = web.perform(post("/reservas").session(sesion).with(csrf())
                        .param("dni", "00000001").param("nombres", "Persona").param("apellidos", "Prueba")
                        .param("telefono", "900000001").param("correo", "persona@example.test")
                        .param("fecha", fecha.toString()).param("horaInicio", "14:00")
                        .param("numeroPersonas", "3").param("idPiso", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/reservas/pago-demo?intento=*"))
                .andReturn();

        web.perform(get(java.net.URI.create(envio.getResponse().getRedirectedUrl())).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagar S/ 60.00")))
                .andExpect(content().string(containsString("S/ 60")));
    }

    @Test
    @Transactional
    void bloqueaPisoConConsultaCompatible() {
        Piso piso = new Piso();
        piso.setNombre("Piso de prueba");
        piso.setEstado(Piso.EstadoPiso.ACTIVO);
        Integer id = pisos.saveAndFlush(piso).getIdPiso();

        assertEquals(id, pisos.bloquearPorId(id).orElseThrow().getIdPiso());
    }

	@Test
	void contextLoads() {
	}

}
