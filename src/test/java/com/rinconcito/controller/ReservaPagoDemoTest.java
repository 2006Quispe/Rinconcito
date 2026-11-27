package com.rinconcito.controller;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Evento;
import com.rinconcito.entity.Reserva;
import com.rinconcito.repository.PisoRepository;
import com.rinconcito.service.PagoReservaService;
import com.rinconcito.service.ReservaService;
import com.rinconcito.service.ReservaService.MetodoDemo;
import com.rinconcito.service.correo.ColaCorreoService;

class ReservaPagoDemoTest {

    @Test
    void rechazoPermiteReintentarYLaAprobacionEsIdempotenteEnLaSesion() {
        ReservaController controlador = new ReservaController();
        ReservaService servicio = mock(ReservaService.class);
        ColaCorreoService correo = mock(ColaCorreoService.class);
        
        // 👇 NUEVO: Simulamos el servicio de pagos
        PagoReservaService pagoReservaService = mock(PagoReservaService.class);

        // Inyectamos los servicios simulados
        ReflectionTestUtils.setField(controlador, "reservaService", servicio);
        ReflectionTestUtils.setField(controlador, "pisoRepository", mock(PisoRepository.class));
        ReflectionTestUtils.setField(controlador, "colaCorreoService", correo);
        ReflectionTestUtils.setField(controlador, "pagoDemoHabilitado", true);
        // 👇 NUEVO: Inyectamos el mock del servicio de pagos
        ReflectionTestUtils.setField(controlador, "pagoReservaService", pagoReservaService);

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
        
        // Simulamos la confirmación de la tarjeta
        when(servicio.confirmarPagoDemo(formulario, null, 60, MetodoDemo.TARJETA)).thenReturn(confirmada);
        
        // 👇 NUEVO: Cuando el controlador busque el código, le devolvemos la reserva simulada
        when(pagoReservaService.buscarPorCodigo("RES-DEMO-01")).thenReturn(Optional.of(confirmada));

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
        controlador.procesarPagoDemo(intento, MetodoDemo.TARJETA, false, null, sesion, null, rechazo);
        assertTrue(rechazo.getFlashAttributes().containsKey("error"));
        verify(servicio, never()).confirmarPagoDemo(any(), any(), anyInt(), any());

        controlador.procesarPagoDemo(intento, MetodoDemo.TARJETA, true, null, sesion, null, new RedirectAttributesModelMap());
        controlador.procesarPagoDemo(intento, MetodoDemo.TARJETA, true, null, sesion, null, new RedirectAttributesModelMap());
        
        verify(servicio).confirmarPagoDemo(formulario, null, 60, MetodoDemo.TARJETA);

        var pantalla = new ExtendedModelMap();
        controlador.mostrarPagoDemo(intento, sesion, null, pantalla);
        assertSame(confirmada, pantalla.get("reserva"));
        assertEquals(60, pantalla.get("importe"));
    }
}