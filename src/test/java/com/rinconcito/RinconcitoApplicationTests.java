package com.rinconcito;

import com.rinconcito.entity.Piso;
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
