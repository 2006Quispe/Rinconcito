package com.rinconcito.service;

import com.rinconcito.dto.ReservaFormDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pago.MetodoPago;
import com.rinconcito.entity.Reserva;
import com.rinconcito.entity.Reserva.EstadoReserva;
import com.rinconcito.entity.Usuario;
import com.rinconcito.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PagoReservaModuloIT {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PagoReservaService pagoReservaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private LocalDate proximo(DayOfWeek dia) {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek() != dia) {
            d = d.plusDays(1);
        }
        if (d.equals(LocalDate.now()) && LocalTime.now().isAfter(LocalTime.of(16, 0))) {
            d = d.plusWeeks(1);
        }
        return d;
    }

    private ReservaFormDTO formPago(String dni) {
        ReservaFormDTO dto = new ReservaFormDTO();
        dto.setDni(dni);
        dto.setNombres("Pago");
        dto.setApellidos("Test");
        dto.setTelefono("999777666");
        dto.setFecha(proximo(DayOfWeek.SUNDAY));
        dto.setHoraInicio(LocalTime.of(14, 0));
        dto.setNumeroPersonas(2);
        dto.setIdPiso(1);
        return dto;
    }

    private MockMultipartFile captura() {
        // Minimal JPEG header bytes + padding
        byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        return new MockMultipartFile("captura", "voucher.jpg", "image/jpeg", bytes);
    }

    @Test
    void evidenciaPasaAPendienteValidacionSinMesa() {
        Reserva r = reservaService.crearReserva(formPago("30000001"));
        assertEquals(EstadoReserva.PENDIENTE_PAGO, r.getEstado());
        assertNull(r.getMesa());

        Pago pago = pagoReservaService.registrarEvidencia(
                r.getCodigoReserva(), MetodoPago.YAPE, null, captura());

        assertEquals(EstadoPago.PENDIENTE_VALIDACION, pago.getEstado());
        assertNotNull(pago.getEvidenciaPago());
        assertFalse(pago.getEvidenciaPago().contains("/"));

        Reserva actualizada = pagoReservaService.buscarPorCodigo(r.getCodigoReserva()).orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE_VALIDACION, actualizada.getEstado());
        assertNull(actualizada.getMesa());
    }

    @Test
    void aprobarAsignaMesaYConfirma() {
        Reserva r = reservaService.crearReserva(formPago("30000002"));
        Pago pago = pagoReservaService.registrarEvidencia(
                r.getCodigoReserva(), MetodoPago.PLIN, "OP-TEST-30000002", captura());

        Usuario caja = usuarioRepository.findByCorreo("caja@rinconcito.pe")
                .orElseGet(() -> usuarioRepository.findAll().stream()
                        .filter(u -> u.getRol() != null && "CAJA".equalsIgnoreCase(u.getRol().getNombre()))
                        .findFirst()
                        .orElseThrow());

        Reserva confirmada = pagoReservaService.aprobar(pago.getIdPago(), caja);
        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
        assertNotNull(confirmada.getMesa());

        Pago aprobado = pagoReservaService.ultimoPagoDe(confirmada).orElseThrow();
        assertEquals(EstadoPago.APROBADO, aprobado.getEstado());
        assertNotNull(aprobado.getFechaValidacion());
        assertNotNull(aprobado.getUsuarioValidador());
    }

    @Test
    void rechazarVuelveAPendientePagoYPermiteReenvio() {
        Reserva r = reservaService.crearReserva(formPago("30000003"));
        Pago pago = pagoReservaService.registrarEvidencia(
                r.getCodigoReserva(), MetodoPago.YAPE, null, captura());

        Usuario caja = usuarioRepository.findByCorreo("caja@rinconcito.pe")
                .orElseGet(() -> usuarioRepository.findAll().stream()
                        .filter(u -> u.getRol() != null && "CAJA".equalsIgnoreCase(u.getRol().getNombre()))
                        .findFirst()
                        .orElseThrow());

        pagoReservaService.rechazar(pago.getIdPago(), caja, "Captura ilegible");

        Reserva otraVez = pagoReservaService.buscarPorCodigo(r.getCodigoReserva()).orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE_PAGO, otraVez.getEstado());

        Pago rechazo = pagoReservaService.ultimoPagoDe(otraVez).orElseThrow();
        assertEquals(EstadoPago.RECHAZADO, rechazo.getEstado());
        assertEquals("Captura ilegible", rechazo.getObservacionValidacion());

        Pago reenvio = pagoReservaService.registrarEvidencia(
                r.getCodigoReserva(), MetodoPago.PLIN, "OP-REENVIO-30000003", captura());
        assertEquals(EstadoPago.PENDIENTE_VALIDACION, reenvio.getEstado());
        assertEquals(EstadoReserva.PENDIENTE_VALIDACION,
                pagoReservaService.buscarPorCodigo(r.getCodigoReserva()).orElseThrow().getEstado());
    }
}
