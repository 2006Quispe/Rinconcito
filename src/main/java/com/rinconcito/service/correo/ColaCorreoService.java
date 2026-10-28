package com.rinconcito.service.correo;

import com.rinconcito.entity.*;
import com.rinconcito.repository.DetallePedidoRepository;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

/** Guarda el aviso en la misma transaccion del pedido, reserva o nota. */
@Service
public class ColaCorreoService {
    private final JdbcTemplate jdbc;
    private final DetallePedidoRepository detalles;
    private final boolean habilitado;

    public ColaCorreoService(JdbcTemplate jdbc, DetallePedidoRepository detalles,
                            @Value("${app.correo.habilitado:false}") boolean habilitado) {
        this.jdbc = jdbc; this.detalles = detalles; this.habilitado = habilitado;
    }

    @Transactional
    public void pedido(Pedido pedido, String destinatario) {
        if (!habilitado) return;
        StringBuilder texto = new StringBuilder("Hemos registrado tu pedido ")
                .append(pedido.getCodigoPedido()).append(".\nEstado: ").append(pedido.getEstado())
                .append("\nModalidad: ").append(pedido.getTipoPedido()).append("\n\nProductos:\n");
        for (DetallePedido detalle : detalles.findByPedido(pedido)) {
            texto.append(detalle.getCantidad()).append(" x ").append(detalle.getProducto().getNombre())
                    .append(" - S/ ").append(detalle.getSubtotal()).append("\n");
        }
        texto.append("\nTotal: S/ ").append(pedido.getTotal())
                .append(pedido.getEstado() == Pedido.EstadoPedido.PENDIENTE_PAGO
                        ? "\nEl pedido aun esta pendiente de pago. Recibiras la nota de venta cuando Caja apruebe el pago."
                        : "\nRecibiras la nota de venta cuando Caja apruebe el pago.");
        guardar("PEDIDO", pedido.getIdPedido(), destinatario,
                "Pedido registrado - " + pedido.getCodigoPedido(), texto.toString(), false);
    }

    @Transactional
    public void reserva(Reserva reserva, String destinatario) {
        if (!habilitado) return;
        String texto = "Hemos registrado tu reserva " + reserva.getCodigoReserva()
                + ".\nEstado: " + reserva.getEstado() + "\nFecha: " + reserva.getFechaReserva()
                + "\nHorario: " + reserva.getHoraInicio() + " a " + reserva.getHoraFin()
                + "\nPersonas: " + reserva.getNumeroPersonas()
                + "\nPiso: " + reserva.getPisoSolicitado().getNombre()
                + (reserva.getMesa() == null ? "" : "\nMesa: " + reserva.getMesa().getNumero())
                + (reserva.isRequierePago() ? "\nImporte pendiente: S/ " + reserva.getMontoReserva()
                : "\nTu reserva esta confirmada y no requiere pago de entrada.");
        guardar("RESERVA", reserva.getIdReserva(), destinatario,
                "Reserva registrada - " + reserva.getCodigoReserva(), texto, false);
    }

    @Transactional
    public void nota(NotaVenta nota, boolean reenvio) {
        if (!habilitado) return;
        if (nota.getEstado() != NotaVenta.EstadoNotaVenta.EMITIDA
                || nota.getPago().getEstado() != Pago.EstadoPago.APROBADO) {
            throw new IllegalStateException("Solo se envian notas emitidas con pago aprobado.");
        }
        Pago pago = nota.getPago();
        Usuario cliente = pago.getPedido() != null ? pago.getPedido().getCliente() : pago.getReserva().getCliente();
        String claveOperacion = pago.getPedido() != null ? "PEDIDO:" + pago.getPedido().getIdPedido()
                : "RESERVA:" + pago.getReserva().getIdReserva();
        var avisos = jdbc.queryForList("SELECT destinatario FROM correo_salida WHERE clave=?", claveOperacion);
        // Conserva el contacto usado al registrar la operacion; no cambia correos de cuentas ajenas.
        String destinatario = !reenvio && !avisos.isEmpty() ? (String) avisos.getFirst().get("destinatario") : cliente.getCorreo();
        String codigo = pago.getPedido() != null ? pago.getPedido().getCodigoPedido() : pago.getReserva().getCodigoReserva();
        String numero = nota.getSerie() + "-" + String.format("%06d", nota.getNumero());
        guardar("NOTA", nota.getIdNota(), destinatario, "Pago confirmado - " + codigo,
                "Hola " + nota.getClienteNombre() + ",\n\nTu pago de S/ " + nota.getTotal()
                + " fue aprobado.\nOperacion: " + codigo + "\nNota de venta: " + numero
                + "\nAdjuntamos tu nota de venta en PDF.\n\nGracias por elegir El Rinconcito.", reenvio);
    }

    private void guardar(String tipo, Integer referencia, String correo, String asunto, String cuerpo, boolean reenvio) {
        String destinatario = correo == null ? "" : correo.trim();
        String estado = correoValido(destinatario) ? "PENDIENTE" : "OMITIDO";
        String error = estado.equals("OMITIDO") ? "El cliente no tiene un correo utilizable. Actualiza sus datos antes de reenviar." : null;
        String clave = tipo + ":" + referencia;
        jdbc.update("""
                INSERT INTO correo_salida (clave,tipo,id_referencia,destinatario,asunto,cuerpo,estado,error)
                VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE clave=clave
                """, clave, tipo, referencia, destinatario, asunto, cuerpo, estado, error);
        if (reenvio) {
            int actualizados = jdbc.update("""
                    UPDATE correo_salida SET destinatario=?,asunto=?,cuerpo=?,estado=?,error=?,intentos=0,
                    proximo_intento=CURRENT_TIMESTAMP WHERE clave=? AND estado <> 'ENVIANDO'
                    """, destinatario, asunto, cuerpo, estado, error, clave);
            if (actualizados == 0) throw new IllegalStateException("El correo ya se esta enviando. Espera unos segundos.");
        }
    }

    public static boolean correoValido(String correo) {
        if (correo == null || correo.isBlank() || correo.length() > 120 || correo.contains("\r") || correo.contains("\n")) return false;
        String normal = correo.toLowerCase(java.util.Locale.ROOT);
        if (normal.endsWith("@invitado.rinconcito.pe") || normal.endsWith(".local")) return false;
        try {
            InternetAddress direccion = new InternetAddress(correo, true);
            direccion.validate();
            return correo.equals(direccion.getAddress()) && correo.substring(correo.lastIndexOf('@') + 1).contains(".");
        } catch (Exception e) { return false; }
    }

    public List<Map<String,Object>> listar() {
        if (!habilitado) return List.of();
        return jdbc.queryForList("SELECT id,tipo,id_referencia,destinatario,asunto,estado,intentos,error,creado,enviado FROM correo_salida ORDER BY id DESC LIMIT 100");
    }

    public void reintentar(long id) {
        if (!habilitado) throw new IllegalStateException("El envio de correo no esta habilitado.");
        int modificados = jdbc.update("""
                UPDATE correo_salida SET estado='PENDIENTE',intentos=0,error=NULL,proximo_intento=CURRENT_TIMESTAMP
                WHERE id=? AND estado='ERROR'
                """, id);
        if (modificados == 0) throw new IllegalStateException("Solo se pueden reintentar correos con error.");
    }
}
