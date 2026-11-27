package com.rinconcito.service;

import com.rinconcito.dto.ClubCobroEstimacionDTO;
import com.rinconcito.dto.ClubConsultaRecompensaDTO;
import com.rinconcito.dto.ClubPagoResumenDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Rol;
import com.rinconcito.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
class ClubCajaFidelizacionTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ClubPuntosService club;
    private Usuario cliente;
    private Usuario cajero;

    @BeforeEach
    void preparar() {
        club = new ClubPuntosService(jdbc);
        cliente = usuario(101, "CLIENTE", "Ada", "Quispe");
        cajero = usuario(201, "CAJA", "Cajero", "Demo");
        jdbc.execute("CREATE TABLE IF NOT EXISTS usuarios (id_usuario INT PRIMARY KEY, nombres VARCHAR(80), apellidos VARCHAR(80))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS pagos (id_pago INT PRIMARY KEY, id_pedido INT, estado VARCHAR(30))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS pedidos (id_pedido INT PRIMARY KEY)");
        jdbc.update("MERGE INTO usuarios (id_usuario, nombres, apellidos) KEY(id_usuario) VALUES (101, 'Ada', 'Quispe')");
        jdbc.update("DELETE FROM club_movimientos");
        jdbc.update("DELETE FROM club_saldos");
        jdbc.update("DELETE FROM club_pago_resumen");
        jdbc.update("DELETE FROM club_campanas");
        jdbc.update("DELETE FROM club_canjes");
        jdbc.update("DELETE FROM club_recompensas");
    }

    @Test
    void estimaLosTresNivelesYConsideraCampanaDescuentoYDeuda() {
        jdbc.update("INSERT INTO club_saldos (id_usuario, puntos, puntos_deuda) VALUES (101, 350, 40)");
        insertarPuntosAnuales(500);
        jdbc.update("""
                INSERT INTO club_campanas (id_campana, nombre, porcentaje_bonus, fecha_inicio, fecha_fin, activa)
                VALUES (1, 'Campana demo', 10, TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2030-01-01 00:00:00', TRUE)
                """);

        Pedido pedido = pedido(100, 20);
        ClubCobroEstimacionDTO amigo = club.previsualizarCobro(cliente, pedido);
        assertThat(amigo.nivelCodigo()).isEqualTo("AMIGO");
        assertThat(amigo.puntosBase()).isEqualTo(80);
        assertThat(amigo.puntosBonusNivel()).isEqualTo(8);
        assertThat(amigo.puntosBonusCampana()).isEqualTo(8);
        assertThat(amigo.totalPuntosEstimados()).isEqualTo(96);
        assertThat(amigo.puntosAplicadosDeuda()).isEqualTo(40);
        assertThat(amigo.saldoNuevoEstimado()).isEqualTo(406);
        assertThat(amigo.deudaNuevaEstimada()).isZero();

        jdbc.update("DELETE FROM club_movimientos");
        insertarPuntosAnuales(0);
        ClubCobroEstimacionDTO visitante = club.previsualizarCobro(cliente, pedido(100, 0));
        assertThat(visitante.nivelCodigo()).isEqualTo("VISITANTE");
        assertThat(visitante.porcentajeBonusNivel()).isZero();

        jdbc.update("DELETE FROM club_movimientos");
        insertarPuntosAnuales(1500);
        ClubCobroEstimacionDTO familia = club.previsualizarCobro(cliente, pedido(100, 0));
        assertThat(familia.nivelCodigo()).isEqualTo("FAMILIA");
        assertThat(familia.porcentajeBonusNivel()).isEqualTo(20);
    }

    @Test
    void noRegistraClienteInexistenteYNoAcreditaPagosNoAprobados() {
        ClubCobroEstimacionDTO noRegistrado = club.previsualizarCobro(null, pedido(120, 0));
        assertThat(noRegistrado.clienteRegistrado()).isFalse();

        Pago rechazado = pago(900, pedido(120, 0), cliente, Pago.EstadoPago.RECHAZADO);
        assertThat(club.acreditarPorPago(rechazado)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_pago_resumen", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_movimientos", Integer.class)).isZero();
    }

    @Test
    void acreditaUnaSolaVezYConservaElResumenHistoricoDelPago() {
        jdbc.update("INSERT INTO club_saldos (id_usuario, puntos, puntos_deuda) VALUES (101, 350, 40)");
        insertarPuntosAnuales(500);
        jdbc.update("""
                INSERT INTO club_campanas (id_campana, nombre, porcentaje_bonus, fecha_inicio, fecha_fin, activa)
                VALUES (1, 'Campana demo', 10, TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2030-01-01 00:00:00', TRUE)
                """);
        Pago aprobado = pago(900, pedido(100, 20), cliente, Pago.EstadoPago.APROBADO);

        ClubPagoResumenDTO primero = club.acreditarPorPago(aprobado);
        ClubPagoResumenDTO segundo = club.acreditarPorPago(aprobado);

        assertThat(primero.puntosBase()).isEqualTo(80);
        assertThat(primero.puntosBonusNivel()).isEqualTo(8);
        assertThat(primero.puntosBonusCampana()).isEqualTo(8);
        assertThat(primero.puntosAplicadosDeuda()).isEqualTo(40);
        assertThat(primero.puntosAcreditados()).isEqualTo(56);
        assertThat(primero.saldoNuevo()).isEqualTo(406);
        assertThat(segundo).isEqualTo(primero);
        assertThat(jdbc.queryForObject("SELECT puntos FROM club_saldos WHERE id_usuario = 101", Integer.class)).isEqualTo(406);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_movimientos WHERE id_pago = 900", Integer.class)).isEqualTo(1);

        jdbc.update("UPDATE club_saldos SET puntos = 999 WHERE id_usuario = 101");
        assertThat(club.obtenerResumenPago(900).saldoNuevo()).isEqualTo(406);
    }

    @Test
    void consultaRecompensasSinConsumirVerificaPropietarioYConfirmaEntregaSeparadamente() {
        jdbc.update("""
                INSERT INTO club_recompensas
                    (id_recompensa, codigo, nombre, descripcion, costo_puntos, tipo, valor, categoria)
                VALUES (1, 'TAZA-LOGO', 'Taza', 'Taza demo', 500, 'PRODUCTO_MERCH', 1, 'MERCHANDISING')
                """);
        jdbc.update("""
                INSERT INTO club_canjes
                    (id_canje, id_usuario, id_recompensa, codigo_cupon, nombre_recompensa, tipo,
                     valor, puntos_usados, estado, vence_en)
                VALUES (1, 101, 1, 'RIN-TAZA101', 'Taza', 'PRODUCTO_MERCH', 1, 500,
                        'PENDIENTE_ENTREGA', TIMESTAMP '2030-01-01 00:00:00')
                """);

        ClubConsultaRecompensaDTO consulta = club.consultarRecompensaEnCaja("RIN-TAZA101", cliente, cajero);
        assertThat(consulta.disponible()).isTrue();
        assertThat(jdbc.queryForObject("SELECT estado FROM club_canjes WHERE id_canje = 1", String.class))
                .isEqualTo("PENDIENTE_ENTREGA");

        Usuario otraCliente = usuario(102, "CLIENTE", "Eva", "Demo");
        assertThat(club.consultarRecompensaEnCaja("RIN-TAZA101", otraCliente, cajero).disponible()).isFalse();
        assertThat(club.confirmarEntregaEnCaja("RIN-TAZA101", cliente, cajero)).isEqualTo("Taza");
        assertThat(jdbc.queryForObject("SELECT estado FROM club_canjes WHERE id_canje = 1", String.class))
                .isEqualTo("USADO");

        jdbc.update("""
                INSERT INTO club_recompensas
                    (id_recompensa, codigo, nombre, descripcion, costo_puntos, tipo, valor, categoria)
                VALUES (2, 'DESCUENTO-5', 'Descuento', 'Online', 100, 'DESCUENTO_FIJO', 5, 'DESCUENTOS')
                """);
        jdbc.update("""
                INSERT INTO club_canjes
                    (id_canje, id_usuario, id_recompensa, codigo_cupon, nombre_recompensa, tipo,
                     valor, puntos_usados, estado, vence_en)
                VALUES (2, 101, 2, 'RIN-ONLINE1', 'Descuento', 'DESCUENTO_FIJO', 5, 100,
                        'DISPONIBLE', TIMESTAMP '2030-01-01 00:00:00')
                """);
        assertThat(club.consultarRecompensaEnCaja("RIN-ONLINE1", cliente, cajero).disponible()).isFalse();
    }

    private void insertarPuntosAnuales(int puntos) {
        if (puntos == 0) return;
        jdbc.update("""
                INSERT INTO club_movimientos (id_usuario, id_pago, tipo, puntos, descripcion)
                VALUES (101, ?, 'ACUMULACION', ?, 'Puntos previos de demostracion')
                """, 700 + puntos, puntos);
    }

    private static Pedido pedido(int subtotal, int descuento) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(500);
        pedido.setSubtotal(subtotal);
        pedido.setDescuentoClub(descuento);
        pedido.setCodigoPedido("TEST-500");
        return pedido;
    }

    private static Pago pago(int id, Pedido pedido, Usuario cliente, Pago.EstadoPago estado) {
        pedido.setCliente(cliente);
        Pago pago = new Pago();
        pago.setIdPago(id);
        pago.setPedido(pedido);
        pago.setEstado(estado);
        return pago;
    }

    private static Usuario usuario(int id, String rolNombre, String nombres, String apellidos) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setRol(rol);
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuario.setTipoRegistro(Usuario.TipoRegistro.CUENTA);
        return usuario;
    }
}
