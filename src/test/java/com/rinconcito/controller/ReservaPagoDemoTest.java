package com.rinconcito.controller;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Reserva;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.service.ReservaService;
import com.rinconcito.service.ReservaService.MetodoDemo;
import com.rinconcito.service.correo.ColaCorreoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservaPagoDemoTest {

    @Test
    void rechazoPermiteReintentarYLaAprobacionEsIdempotenteEnLaSesion() {
        ReservaController controlador = new ReservaController();
        ReservaService servicio = mock(ReservaService.class);
        ColaCorreoService correo = mock(ColaCorreoService.class);
        ReflectionTestUtils.setField(controlador, "reservaService", servicio);
        ReflectionTestUtils.setField(controlador, "pisoRepository", mock(PisoRepository.class));
        ReflectionTestUtils.setField(controlador, "colaCorreoService", correo);
        ReflectionTestUtils.setField(controlador, "pagoDemoHabilitado", true);

        ReservaFormDTO formulario = new ReservaFormDTO();
        formulario.setFecha(LocalDate.now().plusDays(1));
        formulario.setNumeroPersonas(3);
        Evento evento = new Evento();
        evento.setTitulo("Almuerzo show");
        evento.setRequierePago(true);
        evento.setPrecioEntrada(20);
        when(servicio.buscarEvento(formulario.getFecha())).thenReturn(Optional.of(evento));
        Reserva confirmada = new Reserva();
        confirmada.setIdReserva(1);
        confirmada.setCodigoReserva("RES-DEMO-01");
        when(servicio.confirmarPagoDemo(formulario, null, 60, MetodoDemo.YAPE)).thenReturn(confirmada);

        MockHttpSession sesion = new MockHttpSession();
        var redireccion = new RedirectAttributesModelMap();
        assertEquals("redirect:/reservas/pago-demo", controlador.procesarReserva(
                formulario, new ExtendedModelMap(), null, sesion, redireccion));
        String intento = (String) redireccion.get("intento");
        assertNotNull(intento);

        var ajeno = new ExtendedModelMap();
        controlador.mostrarPagoDemo(intento, new MockHttpSession(), null, ajeno);
        assertNotNull(ajeno.get("error"));

        var rechazo = new RedirectAttributesModelMap();
        controlador.procesarPagoDemo(intento, MetodoDemo.PLIN, false, sesion, null, rechazo);
        assertTrue(rechazo.getFlashAttributes().containsKey("error"));
        verify(servicio, never()).confirmarPagoDemo(any(), any(), anyInt(), any());

        controlador.procesarPagoDemo(intento, MetodoDemo.YAPE, true, sesion, null, new RedirectAttributesModelMap());
        controlador.procesarPagoDemo(intento, MetodoDemo.YAPE, true, sesion, null, new RedirectAttributesModelMap());
        verify(servicio).confirmarPagoDemo(formulario, null, 60, MetodoDemo.YAPE);

        var pantalla = new ExtendedModelMap();
        controlador.mostrarPagoDemo(intento, sesion, null, pantalla);
        assertSame(confirmada, pantalla.get("reserva"));
        assertEquals(60, pantalla.get("importe"));
    }
}
