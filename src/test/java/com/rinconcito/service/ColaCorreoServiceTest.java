package com.rinconcito.service;

import com.rinconcito.entity.*;
import com.rinconcito.repository.DetallePedidoRepository;
import com.rinconcito.service.correo.ColaCorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ColaCorreoServiceTest {
    JdbcTemplate jdbc;
    ColaCorreoService cola;
    DetallePedidoRepository detalles;
    TransactionTemplate transaccion;

    @BeforeEach void preparar() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE correo_salida (
                 id BIGINT AUTO_INCREMENT PRIMARY KEY, clave VARCHAR(80) UNIQUE, tipo VARCHAR(20),
                 id_referencia INT, destinatario VARCHAR(254), asunto VARCHAR(200), cuerpo TEXT,
                 estado VARCHAR(20), intentos INT DEFAULT 0, error VARCHAR(300),
                 creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP, enviado TIMESTAMP,
                 proximo_intento TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        detalles = mock(DetallePedidoRepository.class);
        cola = new ColaCorreoService(jdbc, detalles, true);
        transaccion = new TransactionTemplate(new JdbcTransactionManager(source));
    }

    @Test void deshabilitadoNoRequiereTablaNiAccedeALaBase() {
        var sinBase = mock(JdbcTemplate.class);
        var sinDetalles = mock(DetallePedidoRepository.class);
        var deshabilitada = new ColaCorreoService(sinBase, sinDetalles, false);
        deshabilitada.pedido(null, "cliente@example.test");
        deshabilitada.reserva(null, "cliente@example.test");
        deshabilitada.nota(null, false);
        assertTrue(deshabilitada.listar().isEmpty());
        assertNull(deshabilitada.estadoConfirmacion("PEDIDO", 1));
        verifyNoInteractions(sinBase, sinDetalles);
    }

    @Test void avisoConsultaElContactoOriginalYElEstadoRealDelEnvio() {
        assertNull(cola.estadoConfirmacion("RESERVA", 10));
        cola.reserva(reserva(), "cliente@example.test");
        assertEquals("PENDIENTE", cola.estadoConfirmacion("RESERVA", 10));
        jdbc.update("UPDATE correo_salida SET estado='ERROR'");
        assertEquals("ERROR", cola.estadoConfirmacion("RESERVA", 10));
        jdbc.update("UPDATE correo_salida SET estado='ENVIADO'");
        assertEquals("ENVIADO", cola.estadoConfirmacion("RESERVA", 10));
    }

    @Test void noMuestraUnAvisoSinDestinatarioValido() {
        cola.reserva(reserva(), null);
        assertNull(cola.estadoConfirmacion("RESERVA", 10));
    }

    Reserva reserva() {
        var piso = new Piso(); piso.setNombre("Primer piso");
        var reserva = new Reserva(); reserva.setIdReserva(10); reserva.setCodigoReserva("RES-10");
        reserva.setEstado(Reserva.EstadoReserva.CONFIRMADA); reserva.setFechaReserva(LocalDate.of(2026,11,1));
        reserva.setHoraInicio(LocalTime.of(12,0)); reserva.setHoraFin(LocalTime.of(14,0));
        reserva.setNumeroPersonas(4); reserva.setPisoSolicitado(piso); reserva.setRequierePago(false);
        return reserva;
    }

    @Test void registroEsUnicoYExpresaEstadoReal() {
        cola.reserva(reserva(), "cliente@example.test");
        cola.reserva(reserva(), "cliente@example.test");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM correo_salida",Integer.class));
        var fila=cola.listar().getFirst();
        assertEquals("PENDIENTE",fila.get("estado"));
        String cuerpo=jdbc.queryForObject("SELECT cuerpo FROM correo_salida",String.class);
        assertTrue(cuerpo.contains("2026-11-01"));
        assertTrue(cuerpo.contains("CONFIRMADA"));
        assertTrue(cuerpo.contains("no requiere pago"));
    }

    @Test void unRollbackNoDejaUnCorreoPendiente() {
        transaccion.executeWithoutResult(status -> {
            cola.reserva(reserva(),"cliente@example.test");
            status.setRollbackOnly();
        });
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM correo_salida",Integer.class));
    }

    @Test void omiteDestinatariosFalsosInternosYVacios() {
        cola.reserva(reserva(), "12345678@invitado.rinconcito.pe");
        assertEquals("OMITIDO",cola.listar().getFirst().get("estado"));
        assertFalse(ColaCorreoService.correoValido(null));
        assertFalse(ColaCorreoService.correoValido("sin-correo"));
        assertFalse(ColaCorreoService.correoValido("a@b.test\r\nBcc: otra@b.test"));
        assertFalse(ColaCorreoService.correoValido("consumidor.final@rinconcito.local"));
        assertTrue(ColaCorreoService.correoValido("cliente@example.test"));
    }

    @Test void pedidoIncluyeProductosYNoConfirmaUnPagoPendiente() {
        var pedido=new Pedido(); pedido.setIdPedido(1); pedido.setCodigoPedido("PED-1");
        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE_PAGO); pedido.setTipoPedido(Pedido.TipoPedido.RECOJO_LOCAL);
        pedido.setTotal(90);
        var producto=new Producto(); producto.setNombre("Pachamanca");
        var detalle=new DetallePedido(); detalle.setCantidad(2); detalle.setSubtotal(90); detalle.setProducto(producto);
        when(detalles.findByPedido(pedido)).thenReturn(List.of(detalle));
        cola.pedido(pedido,"cliente@example.test");
        String cuerpo=jdbc.queryForObject("SELECT cuerpo FROM correo_salida",String.class);
        assertTrue(cuerpo.contains("2 x Pachamanca - S/ 90"));
        assertTrue(cuerpo.contains("pendiente de pago"));
    }

    @Test void deshabilitadoNoConsultaNiModificaLaBase() {
        var sql=mock(JdbcTemplate.class);
        var desactivada=new ColaCorreoService(sql,detalles,false);
        desactivada.reserva(reserva(),"cliente@example.test");
        desactivada.pedido(new Pedido(),"cliente@example.test");
        assertTrue(desactivada.listar().isEmpty());
        verifyNoInteractions(sql);
    }

    @Test void reintentoEsExplicitoYNoDuplicaFilas() {
        cola.reserva(reserva(),"cliente@example.test");
        long id=((Number)cola.listar().getFirst().get("id")).longValue();
        assertThrows(IllegalStateException.class,()->cola.reintentar(id));
        jdbc.update("UPDATE correo_salida SET estado='ERROR',intentos=3,error='Fallo'");
        cola.reintentar(id);
        assertEquals("PENDIENTE",cola.listar().getFirst().get("estado"));
        assertEquals(0,((Number)cola.listar().getFirst().get("intentos")).intValue());
        assertEquals(1,cola.listar().size());
    }

    @Test void notaNoPuedeEnviarseAntesDeAprobarPago() {
        var pago=new Pago(); pago.setEstado(Pago.EstadoPago.PENDIENTE_VALIDACION);
        var nota=new NotaVenta(); nota.setEstado(NotaVenta.EstadoNotaVenta.EMITIDA); nota.setPago(pago);
        assertThrows(IllegalStateException.class,()->cola.nota(nota,false));
        assertTrue(cola.listar().isEmpty());
    }

    NotaVenta notaAprobada() {
        var cliente=new Usuario(); cliente.setCorreo("cuenta@example.test");
        var reserva=reserva(); reserva.setCliente(cliente);
        var pago=new Pago(); pago.setReserva(reserva); pago.setEstado(Pago.EstadoPago.APROBADO);
        var nota=new NotaVenta(); nota.setIdNota(20); nota.setSerie("NV01"); nota.setNumero(1);
        nota.setEstado(NotaVenta.EstadoNotaVenta.EMITIDA); nota.setPago(pago);
        nota.setClienteNombre("Cliente Demo"); nota.setTotal(50);
        return nota;
    }

    @Test void envioAutomaticoConservaElCorreoDeLaOperacionYSoloEncolaUnaNota() {
        cola.reserva(reserva(),"contacto@example.test");
        var nota=notaAprobada();
        cola.nota(nota,false);
        cola.nota(nota,false);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM correo_salida",Integer.class));
        assertEquals("contacto@example.test",jdbc.queryForObject(
                "SELECT destinatario FROM correo_salida WHERE clave='NOTA:20'",String.class));
    }

    @Test void reenvioExplicitoActualizaContactoSinDuplicarLaNota() {
        var nota=notaAprobada(); cola.nota(nota,false);
        jdbc.update("UPDATE correo_salida SET estado='ENVIADO',intentos=1");
        nota.getPago().getReserva().getCliente().setCorreo("corregido@example.test");
        cola.nota(nota,true);
        assertEquals(1,cola.listar().size());
        assertEquals("corregido@example.test",cola.listar().getFirst().get("destinatario"));
        assertEquals("PENDIENTE",cola.listar().getFirst().get("estado"));
        assertEquals(NotaVenta.EstadoNotaVenta.EMITIDA,nota.getEstado());
    }
}
