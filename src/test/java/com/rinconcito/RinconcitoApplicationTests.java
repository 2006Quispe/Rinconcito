package com.rinconcito;

import com.rinconcito.entity.Piso;
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
                .andExpect(content().string(containsString("Simular pago aprobado")))
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
