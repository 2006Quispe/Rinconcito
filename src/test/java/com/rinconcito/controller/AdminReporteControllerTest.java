package com.rinconcito.controller;

import com.rinconcito.config.SecurityConfig;
import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ArchivoReporteDTO;
import com.rinconcito.service.*;
import com.rinconcito.service.reporte.FormatoReporteNoSoportadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(AdminReporteControllerTest.Config.class)
class AdminReporteControllerTest {
    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean AdminReporteService consulta() { return mock(AdminReporteService.class); }
        @Bean ExportacionReporteService exportacion() { return mock(ExportacionReporteService.class); }
        @Bean AdminReporteController controlador(AdminReporteService consulta, ExportacionReporteService exportacion) {
            return new AdminReporteController(consulta, exportacion);
        }
    }
    @Autowired WebApplicationContext contexto;
    @Autowired AdminReporteService consulta;
    @Autowired ExportacionReporteService exportacion;
    MockMvc web;
    AdminReporteDTO reporte;
    @BeforeEach void preparar() {
        reset(consulta, exportacion);
        reporte = mock(AdminReporteDTO.class);
        when(consulta.generar(any(), any())).thenReturn(reporte);
        when(exportacion.exportar(eq(reporte), any())).thenReturn(new ArchivoReporteDTO(new byte[]{1,2}, "reporte.pdf", "application/pdf"));
        web = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }
    @Test void mantieneRutasYCabeceras() throws Exception {
        for (String ruta : new String[]{"/pdf", "/excel", "/exportar?formato=pdf"}) {
            web.perform(get("/admin/reportes" + ruta).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1,2}))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", "2"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"));
        }
        verify(exportacion, times(2)).exportar(reporte, "pdf");
        verify(exportacion).exportar(reporte, "excel");
        verify(consulta, times(3)).generar(null, null);
    }
    @Test void conservaProteccionEnTodasLasRutas() throws Exception {
        for (String ruta : new String[]{"/pdf", "/excel", "/exportar?formato=pdf"}) {
            web.perform(get("/admin/reportes" + ruta)).andExpect(status().is3xxRedirection());
            web.perform(get("/admin/reportes" + ruta).with(user("cliente").roles("CLIENTE")))
                .andExpect(status().is3xxRedirection());
        }
        verifyNoInteractions(consulta, exportacion);
    }
    @Test void distingueErroresDeSolicitudYGeneracion() throws Exception {
        when(exportacion.exportar(reporte, "otro")).thenThrow(new FormatoReporteNoSoportadoException());
        web.perform(get("/admin/reportes/exportar?formato=otro").with(user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isBadRequest());
        web.perform(get("/admin/reportes/exportar").with(user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isBadRequest());
        when(exportacion.exportar(reporte, "pdf")).thenThrow(new IllegalStateException("Error tecnico"));
        web.perform(get("/admin/reportes/pdf").with(user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isInternalServerError());
        when(consulta.generar(any(), any())).thenThrow(new IllegalStateException("Periodo invalido"));
        web.perform(get("/admin/reportes/pdf").with(user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isBadRequest());
    }
}
