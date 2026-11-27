package com.rinconcito.service;

import com.rinconcito.dto.ClubCuponDTO;
import com.rinconcito.dto.ClubCobroEstimacionDTO;
import com.rinconcito.dto.ClubConsultaRecompensaDTO;
import com.rinconcito.dto.ClubNivelDTO;
import com.rinconcito.dto.ClubMovimientoDTO;
import com.rinconcito.dto.ClubPagoResumenDTO;
import com.rinconcito.dto.ClubProgresoDTO;
import com.rinconcito.dto.ClubRecompensaDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pago.EstadoPago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Pedido.TipoPedido;
import com.rinconcito.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ClubPuntosService {

    private static final int PAGE_SIZE = 20;
    private static final String RECOMPENSA_SQL = """
            SELECT r.id_recompensa, r.codigo, r.nombre, r.descripcion,
                   r.costo_puntos, r.tipo, r.categoria, r.valor,
                   r.compra_minima, r.condiciones,
                   COALESCE(NULLIF(r.imagen, ''), p.imagen) AS imagen,
                   r.stock, r.vigencia_dias, r.id_producto, r.precio_referencia,
                   r.disponible_desde, r.disponible_hasta, r.activo,
                   p.estado AS estado_producto,
                   p.disponible AS producto_disponible
            FROM club_recompensas r
            LEFT JOIN productos p ON p.id_producto = r.id_producto
            """;

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.club.puntos-por-sol:1}")
    private int puntosPorSolPorDefecto;

    @Value("${app.delivery.costo-soles:0}")
    private int costoDeliveryConfigurado;

    public ClubPuntosService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int obtenerSaldo(Usuario cliente) {
        validarCliente(cliente);
        asegurarSaldo(cliente.getIdUsuario());
        expirarPuntosUsuario(cliente.getIdUsuario());
        return saldoBloqueado(cliente.getIdUsuario());
    }

    @Transactional(readOnly = true)
    public List<ClubRecompensaDTO> listarRecompensas(int saldo) {
        return jdbcTemplate.query(
                RECOMPENSA_SQL + " WHERE r.activo = 1 ORDER BY r.costo_puntos ASC, r.nombre ASC, r.codigo ASC",
                (rs, fila) -> mapearRecompensa(rs, fila, saldo)
        );
    }

    @Transactional(readOnly = true)
    public ClubProgresoDTO calcularProgreso(
            int saldo,
            List<ClubRecompensaDTO> recompensas
    ) {
        List<ClubRecompensaDTO> disponibles = recompensas.stream()
                .filter(recompensa -> !"AGOTADA".equals(recompensa.estadoDisponibilidad())
                        && !"PROXIMAMENTE".equals(recompensa.estadoDisponibilidad())
                        && !"VENCIDA".equals(recompensa.estadoDisponibilidad())
                        && !"CONFIGURACION_PENDIENTE".equals(recompensa.estadoDisponibilidad())
                        && !"NO_DISPONIBLE_EN_CARTA".equals(recompensa.estadoDisponibilidad()))
                .sorted((a, b) -> Integer.compare(a.costoPuntos(), b.costoPuntos()))
                .toList();

        ClubRecompensaDTO objetivo = disponibles.stream()
                .filter(ClubRecompensaDTO::canjeable)
                .findFirst()
                .orElseGet(() -> disponibles.stream()
                        .filter(recompensa -> recompensa.costoPuntos() > saldo)
                        .findFirst()
                        .orElse(null));

        if (objetivo == null) {
            return new ClubProgresoDTO(
                    null, saldo, 0, 0, 0, false,
                    "Estamos preparando nuevas recompensas para ti."
            );
        }

        int faltantes = Math.max(0, objetivo.costoPuntos() - saldo);
        int progreso = objetivo.costoPuntos() <= 0
                ? 100
                : (int) Math.min(100L, (long) saldo * 100 / objetivo.costoPuntos());
        boolean puedeCanjear = faltantes == 0;
        String mensaje = puedeCanjear
                ? "Ya puedes canjear " + objetivo.nombre() + "."
                : "¡Solo te faltan " + faltantes + " puntos para disfrutar "
                        + objetivo.nombre() + "!";

        return new ClubProgresoDTO(
                objetivo, saldo, objetivo.costoPuntos(), faltantes,
                progreso, puedeCanjear, mensaje
        );
    }

    @Transactional(readOnly = true)
    public List<ClubCuponDTO> listarCanjes(Usuario cliente) {
        validarCliente(cliente);
        return jdbcTemplate.query("""
                SELECT c.id_canje, c.nombre_recompensa, c.codigo_cupon,
                       CASE WHEN c.estado IN ('DISPONIBLE', 'PENDIENTE_ENTREGA')
                                  AND c.vence_en <= NOW()
                            THEN 'VENCIDO' ELSE c.estado END AS estado,
                       c.puntos_usados, c.fecha_canje, c.vence_en,
                       c.tipo, c.categoria, c.valor, c.compra_minima,
                       c.condiciones
                FROM club_canjes c
                WHERE c.id_usuario = ?
                ORDER BY c.fecha_canje DESC, c.id_canje DESC
                """, this::mapearCanje, cliente.getIdUsuario());
    }

    @Transactional(readOnly = true)
    public List<ClubMovimientoDTO> listarMovimientos(
            Usuario cliente,
            int pagina
    ) {
        validarCliente(cliente);
        int offset = Math.max(0, pagina) * PAGE_SIZE;
        return jdbcTemplate.query("""
                SELECT tipo, puntos, descripcion, referencia_operacion,
                       creado_en, vence_en
                FROM club_movimientos
                WHERE id_usuario = ?
                ORDER BY creado_en DESC, id_movimiento DESC
                LIMIT ? OFFSET ?
                """, (rs, fila) -> new ClubMovimientoDTO(
                rs.getString("tipo"),
                rs.getInt("puntos"),
                rs.getString("descripcion"),
                rs.getString("referencia_operacion"),
                fechaNullable(rs, "creado_en"),
                fechaNullable(rs, "vence_en")
        ), cliente.getIdUsuario(), PAGE_SIZE, offset);
    }

    @Transactional(readOnly = true)
    public int contarMovimientos(Usuario cliente) {
        validarCliente(cliente);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM club_movimientos WHERE id_usuario = ?",
                Integer.class,
                cliente.getIdUsuario()
        );
        return total == null ? 0 : total;
    }

    @Transactional
    public String canjear(Usuario cliente, Integer idRecompensa) {
        validarCliente(cliente);
        if (idRecompensa == null) {
            throw new IllegalStateException("Selecciona una recompensa válida.");
        }

        asegurarSaldo(cliente.getIdUsuario());
        int saldo = saldoBloqueado(cliente.getIdUsuario());
        ClubRecompensaDTO recompensa = jdbcTemplate.query(
                RECOMPENSA_SQL + " WHERE r.id_recompensa = ? AND r.activo = 1 FOR UPDATE",
                (rs, fila) -> mapearRecompensa(rs, fila, saldo),
                idRecompensa
        ).stream().findFirst().orElseThrow(() ->
                new IllegalStateException("Esta recompensa ya no existe.")
        );

        if (!"DISPONIBLE".equals(recompensa.estadoDisponibilidad())) {
            throw new IllegalStateException(mensajeNoDisponible(recompensa));
        }
        if (requiereCanjeEntreSemana(recompensa.codigo())
                && LocalDate.now(ZoneId.of("America/Lima")).getDayOfWeek().getValue() > 5) {
            throw new IllegalStateException("Este canje solo esta disponible de lunes a viernes.");
        }
        if (saldo < recompensa.costoPuntos()) {
            throw new IllegalStateException(
                    "Te faltan " + (recompensa.costoPuntos() - saldo)
                            + " puntos para canjear esta recompensa."
            );
        }

        String codigo = nuevoCodigoCupon();
        String estado = esRecompensaFisica(recompensa.tipo())
                ? "PENDIENTE_ENTREGA"
                : "DISPONIBLE";
        int vigencia = recompensa.vigenciaDias() != null
                ? recompensa.vigenciaDias()
                : configuracion("VIGENCIA_RECOMPENSAS_DIAS", 90);

        jdbcTemplate.update("""
                INSERT INTO club_canjes
                    (id_usuario, id_recompensa, codigo_cupon,
                     nombre_recompensa, tipo, valor, compra_minima,
                     puntos_usados, estado, vence_en, condiciones, categoria)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,
                        DATE_ADD(NOW(), INTERVAL ? DAY), ?, ?)
                """, cliente.getIdUsuario(), recompensa.idRecompensa(),
                codigo, recompensa.nombre(), recompensa.tipo(), recompensa.valor(),
                recompensa.compraMinima(), recompensa.costoPuntos(), estado,
                vigencia, recompensa.condiciones(), recompensa.categoria());

        Long idCanje = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (idCanje == null) {
            throw new IllegalStateException("No se pudo generar el código de canje.");
        }

        consumirLotes(cliente.getIdUsuario(), recompensa.costoPuntos(), idCanje);
        int saldoActualizado = jdbcTemplate.update("""
                UPDATE club_saldos
                SET puntos = puntos - ?
                WHERE id_usuario = ? AND puntos >= ?
                """, recompensa.costoPuntos(), cliente.getIdUsuario(), recompensa.costoPuntos());
        if (saldoActualizado != 1) {
            throw new IllegalStateException("El saldo cambió. Actualiza la página e inténtalo otra vez.");
        }

        jdbcTemplate.update("""
                INSERT INTO club_movimientos
                    (id_usuario, id_canje, tipo, puntos, descripcion,
                     referencia_operacion, id_admin)
                VALUES (?, ?, 'CANJE', ?, ?, ?, NULL)
                """, cliente.getIdUsuario(), idCanje,
                -recompensa.costoPuntos(), "Canje: " + recompensa.nombre(), codigo);

        if (recompensa.stock() != null) {
            int descontado = jdbcTemplate.update("""
                    UPDATE club_recompensas
                    SET stock = stock - 1
                    WHERE id_recompensa = ? AND stock > 0
                    """, recompensa.idRecompensa());
            if (descontado != 1) {
                throw new IllegalStateException("La recompensa se agotó. No se descontaron tus puntos.");
            }
        }

        return codigo;
    }

    @Transactional
    public AplicacionCuponPedido reservarCuponParaPedido(
            String codigoCupon, Usuario cliente, int subtotal, TipoPedido tipoPedido, int costoDelivery
    ) {
        if (codigoCupon == null || codigoCupon.isBlank()) return AplicacionCuponPedido.vacia();
        validarCliente(cliente);
        List<ClubCuponDTO> encontrados = jdbcTemplate.query("""
                SELECT id_canje, nombre_recompensa, codigo_cupon, estado,
                       puntos_usados, fecha_canje, vence_en, tipo, categoria,
                       valor, compra_minima, condiciones
                FROM club_canjes WHERE codigo_cupon = ? AND id_usuario = ? FOR UPDATE
                """, this::mapearCanje, codigoCupon.trim().toUpperCase(Locale.ROOT), cliente.getIdUsuario());
        ClubCuponDTO cupon = encontrados.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No encontramos ese cupón en tu cuenta."));
        boolean productoClub = List.of("PRODUCTO_CARTA", "PRODUCTO_MERCH").contains(cupon.tipo());
        boolean estadoValido = "DISPONIBLE".equals(cupon.estado())
                || productoClub && "PENDIENTE_ENTREGA".equals(cupon.estado());
        if (!estadoValido || cupon.venceEn() == null || !cupon.venceEn().isAfter(LocalDateTime.now()))
            throw new IllegalStateException("Ese cupón ya fue usado, venció o está aplicado a otro pedido.");
        if ("EXPERIENCIA".equals(cupon.tipo()))
            throw new IllegalStateException("Las experiencias familiares se validan exclusivamente en Caja.");
        if (subtotal < cupon.compraMinima())
            throw new IllegalStateException("Este cupón requiere un pedido mínimo de S/ " + cupon.compraMinima() + ".");

        int descuento;
        RecompensaProductoPedido productoGratis = null;
        if (productoClub) {
            List<RewardConfig> configs = jdbcTemplate.query("""
                    SELECT r.id_producto, r.precio_referencia, r.activo, r.stock,
                           p.nombre AS producto_nombre, p.precio AS producto_precio,
                           p.estado AS producto_estado, p.disponible AS producto_disponible
                    FROM club_canjes c JOIN club_recompensas r ON r.id_recompensa = c.id_recompensa
                    LEFT JOIN productos p ON p.id_producto = r.id_producto
                    WHERE c.id_canje = ? FOR UPDATE
                    """, (rs, fila) -> new RewardConfig(rs.getObject("id_producto", Integer.class),
                    rs.getObject("precio_referencia", Integer.class), rs.getBoolean("activo"),
                    rs.getObject("stock", Integer.class), rs.getString("producto_nombre"),
                    rs.getObject("producto_precio", Integer.class), rs.getString("producto_estado"),
                    rs.getObject("producto_disponible", Boolean.class)), cupon.idCanje());
            if (configs.isEmpty() || !configs.getFirst().activo())
                throw new IllegalStateException("La recompensa ya no está disponible.");
            RewardConfig config = configs.getFirst();
            Integer idProducto = config.idProducto();
            String nombre = cupon.nombreRecompensa();
            Integer precio = config.precioReferencia();
            if ("PRODUCTO_CARTA".equals(cupon.tipo())) {
                if (idProducto == null || !"ACTIVO".equals(config.productoEstado())
                        || !Boolean.TRUE.equals(config.productoDisponible())
                        || config.productoPrecio() == null || config.productoPrecio() <= 0)
                    throw new IllegalStateException("El producto de esta recompensa ya no está disponible en la carta.");
                nombre = config.productoNombre(); precio = config.productoPrecio();
            } else idProducto = null;
            if (precio == null || precio <= 0)
                throw new IllegalStateException("El precio habitual de la recompensa no está configurado.");
            descuento = precio;
            productoGratis = new RecompensaProductoPedido(idProducto, nombre, precio);
        } else {
            descuento = switch (cupon.tipo()) {
                case "DESCUENTO_FIJO" -> Math.min(subtotal, cupon.valor());
                case "DESCUENTO_PORCENTAJE" -> (int) ((long) subtotal * cupon.valor() / 100);
                case "DELIVERY_GRATIS" -> {
                    if (tipoPedido != TipoPedido.DELIVERY)
                        throw new IllegalStateException("El cupón de delivery gratis solo aplica a pedidos delivery.");
                    if (costoDelivery <= 0) throw new IllegalStateException("La tarifa de delivery aún no está configurada.");
                    yield costoDelivery;
                }
                default -> throw new IllegalStateException("Este canje no se aplica a esta modalidad de pedido.");
            };
        }
        if (descuento <= 0) throw new IllegalStateException("El cupón no genera un descuento válido.");
        int actualizado = jdbcTemplate.update("""
                UPDATE club_canjes SET estado = 'RESERVADO'
                WHERE id_canje = ? AND estado = ?
                """, cupon.idCanje(), cupon.estado());
        if (actualizado != 1) throw new IllegalStateException("Ese cupón ya fue utilizado.");
        return new AplicacionCuponPedido(descuento, productoGratis);
    }

    public record AplicacionCuponPedido(int descuento, RecompensaProductoPedido recompensa) {
        static AplicacionCuponPedido vacia() { return new AplicacionCuponPedido(0, null); }
    }
    public record RecompensaProductoPedido(Integer idProducto, String nombre, int precio) {}
    private record RewardConfig(Integer idProducto, Integer precioReferencia, boolean activo, Integer stock,
                                String productoNombre, Integer productoPrecio, String productoEstado,
                                Boolean productoDisponible) {}

    @Transactional
    public void vincularCuponAPedido(String codigoCupon, Integer idUsuario, Integer idPedido) {
        if (codigoCupon == null || codigoCupon.isBlank()) return;
        int actualizado = jdbcTemplate.update("""
                UPDATE club_canjes SET id_pedido = ?
                WHERE codigo_cupon = ? AND id_usuario = ?
                  AND estado = 'RESERVADO' AND id_pedido IS NULL
                """, idPedido, codigoCupon.trim().toUpperCase(Locale.ROOT), idUsuario);
        if (actualizado != 1) {
            throw new IllegalStateException("No se pudo asociar el cupón al pedido. Intenta nuevamente.");
        }
    }

    @Transactional
    public void confirmarCuponesDelPedido(Integer idPedido) {
        if (idPedido == null) return;
        jdbcTemplate.update("""
                UPDATE club_canjes SET estado = 'USADO', usado_en = NOW()
                WHERE id_pedido = ? AND estado = 'RESERVADO'
                """, idPedido);
    }

    @Transactional
    public void liberarCuponesDelPedido(Integer idPedido) {
        if (idPedido == null) return;
        jdbcTemplate.update("""
                UPDATE club_canjes
                SET estado = IF(vence_en > NOW(), IF(tipo IN ('PRODUCTO_CARTA', 'PRODUCTO_MERCH'), 'PENDIENTE_ENTREGA', 'DISPONIBLE'), 'VENCIDO'),
                    id_pedido = NULL
                WHERE id_pedido = ? AND estado = 'RESERVADO'
                """, idPedido);
    }

    @Transactional
    public ClubCobroEstimacionDTO previsualizarCobro(Usuario cliente, Pedido pedido) {
        if (!esClienteElegible(cliente) || pedido == null) {
            return ClubCobroEstimacionDTO.noRegistrado();
        }

        long anuales = puntosAcumuladosEnAnio(cliente.getIdUsuario());
        ClubNivelDTO nivel = nivelFidelizacion(anuales);
        int saldo = valorSaldo(cliente.getIdUsuario(), "puntos");
        int deuda = valorSaldo(cliente.getIdUsuario(), "puntos_deuda");
        int baseElegible = montoElegiblePedido(pedido);
        CalculoPuntos calculo = calcularPuntos(baseElegible, anuales);
        int total = calculo.total();
        int aplicadoADeuda = Math.min(deuda, total);
        int acreditado = total - aplicadoADeuda;

        return new ClubCobroEstimacionDTO(
                true, "Cliente del Club identificado.", nombreCompleto(cliente),
                nivel.codigo(), nivel.nombre(), nivel.porcentajeBonus(),
                (int) Math.min(Integer.MAX_VALUE, anuales), saldo, deuda,
                calculo.base(), calculo.bonoNivel(), calculo.bonoCampana(),
                calculo.campanaNombre(), total, aplicadoADeuda, acreditado,
                saldo + acreditado, deuda - aplicadoADeuda
        );
    }

    @Transactional
    public ClubPagoResumenDTO acreditarPorPago(Pago pago) {
        if (pago == null || pago.getEstado() != EstadoPago.APROBADO || pago.getPedido() == null) return null;
        Pedido pedido = pago.getPedido();
        confirmarCuponesDelPedido(pedido.getIdPedido());

        Usuario cliente = pedido.getCliente();
        if (!esClienteElegible(cliente) || pago.getIdPago() == null) return null;

        asegurarSaldo(cliente.getIdUsuario());
        int saldoAnterior = saldoBloqueado(cliente.getIdUsuario());
        int deudaAnterior = deudaBloqueada(cliente.getIdUsuario());
        ClubPagoResumenDTO resumenExistente = buscarResumenPago(pago.getIdPago());
        if (resumenExistente != null) return resumenExistente;

        long anualesAntes = puntosAcumuladosEnAnio(cliente.getIdUsuario());
        ClubNivelDTO nivel = nivelFidelizacion(anualesAntes);
        CalculoPuntos calculo = calcularPuntos(montoElegiblePedido(pedido), anualesAntes);
        int base = calculo.base();
        int bonoCampana = calculo.bonoCampana();
        int bonoNivel = calculo.bonoNivel();
        int total = calculo.total();
        CampanaActiva campana = calculo.campanaNombre() == null
                ? null : new CampanaActiva(null, calculo.campanaNombre(), 0);

        String referenciaPago = "PAGO-" + pago.getIdPago();
        if (base > 0) {
            int insertado = jdbcTemplate.update("""
                    INSERT IGNORE INTO club_movimientos
                        (id_usuario, id_pago, tipo, puntos, descripcion, referencia_operacion)
                    VALUES (?, ?, 'ACUMULACION', ?, ?, ?)
                    """, cliente.getIdUsuario(), pago.getIdPago(), base,
                    "Consumo del pedido " + pedido.getCodigoPedido(), referenciaPago);
            if (insertado != 1) return buscarResumenPago(pago.getIdPago());
        }

        if (base > 0 && bonoCampana > 0) {
            jdbcTemplate.update("""
                    INSERT INTO club_movimientos
                        (id_usuario, tipo, puntos, descripcion, referencia_operacion)
                    VALUES (?, 'BONIFICACION', ?, ?, ?)
                    """, cliente.getIdUsuario(), bonoCampana,
                    "Campaña: " + campana.nombre(), "BONO-PAGO-" + pago.getIdPago());
        }

        if (base > 0 && bonoNivel > 0) {
            jdbcTemplate.update("""
                    INSERT INTO club_movimientos
                        (id_usuario, tipo, puntos, descripcion, referencia_operacion)
                    VALUES (?, 'BONIFICACION', ?, ?, ?)
                    """, cliente.getIdUsuario(), bonoNivel,
                    "Nivel " + nivel.nombre() + ": bono adicional del "
                            + nivel.porcentajeBonus() + "%",
                    "BONO-NIVEL-PAGO-" + pago.getIdPago());
        }

        int aplicadoADeuda = Math.min(deudaAnterior, total);
        int acreditado = total - aplicadoADeuda;
        int diasVigencia = Math.max(1, configuracion("VIGENCIA_PUNTOS_DIAS", 365));
        Long idLote = null;
        if (acreditado > 0) {
            idLote = crearLote(
                    cliente.getIdUsuario(), pago.getIdPago(), referenciaPago,
                    acreditado, LocalDateTime.now().plusDays(diasVigencia)
            );
            jdbcTemplate.update("UPDATE club_saldos SET puntos = puntos + ? WHERE id_usuario = ?",
                    acreditado, cliente.getIdUsuario());
        }
        if (aplicadoADeuda > 0) {
            jdbcTemplate.update("UPDATE club_saldos SET puntos_deuda = puntos_deuda - ? WHERE id_usuario = ?",
                    aplicadoADeuda, cliente.getIdUsuario());
            jdbcTemplate.update("""
                    INSERT INTO club_movimientos
                        (id_usuario, id_lote, tipo, puntos, descripcion, referencia_operacion)
                    VALUES (?, ?, 'REVERSO', ?, 'Compensación de puntos pendientes', ?)
                    """, cliente.getIdUsuario(), idLote, -aplicadoADeuda,
                    "COMPENSACION-PAGO-" + pago.getIdPago());
        }
        if (idLote != null) {
            jdbcTemplate.update("""
                    UPDATE club_movimientos SET id_lote = ?, vence_en =
                        (SELECT vence_en FROM club_lotes_puntos WHERE id_lote = ?)
                    WHERE referencia_operacion IN (?, ?, ?)
                    """, idLote, idLote, referenciaPago,
                    "BONO-PAGO-" + pago.getIdPago(),
                    "BONO-NIVEL-PAGO-" + pago.getIdPago());
        }

        int saldoNuevo = saldoAnterior + acreditado;
        int deudaNueva = deudaAnterior - aplicadoADeuda;
        ClubNivelDTO nuevoNivel = nivelFidelizacion(Math.min(
                Integer.MAX_VALUE, anualesAntes + total));
        jdbcTemplate.update("""
                INSERT INTO club_pago_resumen
                    (id_pago, id_usuario, nivel_codigo, nivel_nombre,
                     puntos_anuales_antes, saldo_anterior, deuda_anterior,
                     puntos_base, puntos_bonus_nivel, puntos_bonus_campana,
                     campana_nombre, puntos_totales, puntos_aplicados_deuda,
                     puntos_acreditados, saldo_nuevo, deuda_nueva,
                     nuevo_nivel_codigo, nuevo_nivel_nombre)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, pago.getIdPago(), cliente.getIdUsuario(), nivel.codigo(), nivel.nombre(),
                (int) Math.min(Integer.MAX_VALUE, anualesAntes), saldoAnterior, deudaAnterior,
                base, bonoNivel, bonoCampana, calculo.campanaNombre(), total,
                aplicadoADeuda, acreditado, saldoNuevo, deudaNueva,
                nuevoNivel.codigo(), nuevoNivel.nombre());
        return buscarResumenPago(pago.getIdPago());
    }

    @Transactional(readOnly = true)
    public ClubPagoResumenDTO obtenerResumenPago(Integer idPago) {
        return idPago == null ? null : buscarResumenPago(idPago);
    }

    @Transactional(readOnly = true)
    public ClubPagoResumenDTO obtenerResumenPagoPorPedido(Integer idPedido) {
        if (idPedido == null) return null;
        List<Integer> pagos = jdbcTemplate.queryForList("""
                SELECT p.id_pago FROM pagos p
                JOIN club_pago_resumen r ON r.id_pago = p.id_pago
                WHERE p.id_pedido = ? AND p.estado = 'APROBADO'
                ORDER BY p.id_pago DESC LIMIT 1
                """, Integer.class, idPedido);
        return pagos.isEmpty() ? null : buscarResumenPago(pagos.getFirst());
    }

    @Transactional(readOnly = true)
    public ClubConsultaRecompensaDTO consultarRecompensaEnCaja(
            String codigo, Usuario cliente, Usuario cajero
    ) {
        if (!esCajero(cajero)) {
            return ClubConsultaRecompensaDTO.noDisponible(codigo, "No tienes permiso para consultar canjes.");
        }
        if (!esClienteElegible(cliente)) {
            return ClubConsultaRecompensaDTO.noDisponible(codigo, "Identifica primero a un cliente activo del Club.");
        }
        if (codigo == null || codigo.isBlank()) {
            return ClubConsultaRecompensaDTO.noDisponible(codigo, "Ingresa el código del canje.");
        }

        List<CanjeCaja> filas = jdbcTemplate.query("""
                SELECT c.id_canje, c.id_usuario, c.nombre_recompensa, c.codigo_cupon,
                       c.estado, c.vence_en, c.tipo, c.condiciones, r.codigo AS codigo_recompensa
                FROM club_canjes c
                JOIN club_recompensas r ON r.id_recompensa = c.id_recompensa
                WHERE c.codigo_cupon = ?
                """, (rs, fila) -> new CanjeCaja(
                rs.getLong("id_canje"), rs.getInt("id_usuario"),
                rs.getString("nombre_recompensa"), rs.getString("codigo_cupon"),
                rs.getString("estado"), fechaNullable(rs, "vence_en"),
                rs.getString("tipo"), rs.getString("condiciones"),
                rs.getString("codigo_recompensa")
        ), codigo.trim().toUpperCase(Locale.ROOT));

        CanjeCaja canje = filas.stream().findFirst().orElse(null);
        if (canje == null) {
            return ClubConsultaRecompensaDTO.noDisponible(codigo, "No encontramos ese código de recompensa.");
        }
        String mensaje = validarCanjeCaja(canje, cliente);
        return mensaje == null
                ? new ClubConsultaRecompensaDTO(true, "Recompensa vigente. Aún no se ha consumido.",
                        canje.codigoCupon(), canje.nombre(), canje.condiciones())
                : ClubConsultaRecompensaDTO.noDisponible(canje.codigoCupon(), mensaje);
    }

    @Transactional
    public String confirmarEntregaEnCaja(String codigo, Usuario cliente, Usuario cajero) {
        if (!esClienteElegible(cliente)) {
            throw new IllegalStateException("Identifica primero a un cliente activo del Club.");
        }
        CanjeCaja canje = cargarCanjeCajaBloqueado(codigo);
        String mensaje = validarCanjeCaja(canje, cliente);
        if (mensaje != null) throw new IllegalStateException(mensaje);
        return consumirCanjeEnCaja(canje, cajero);
    }

    @Transactional
    public void revertirPorPedido(Integer idPedido, Usuario administrador, String motivo) {
        validarAdministrador(administrador);
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 8) {
            throw new IllegalStateException("Describe el motivo de la reversión (mínimo 8 caracteres).");
        }
        String estadoPedido = jdbcTemplate.query("""
                SELECT estado FROM pedidos WHERE id_pedido = ? FOR UPDATE
                """, rs -> rs.next() ? rs.getString("estado") : null, idPedido);
        if (!"CANCELADO".equals(estadoPedido)) {
            throw new IllegalStateException("Solo se revierten puntos cuando el pedido está cancelado.");
        }
        List<ReversionPago> pagos = jdbcTemplate.query("""
                SELECT p.id_pago, pe.id_cliente, pe.codigo_pedido
                FROM pagos p
                JOIN pedidos pe ON pe.id_pedido = p.id_pedido
                JOIN usuarios u ON u.id_usuario = pe.id_cliente
                JOIN roles r ON r.id_rol = u.id_rol
                WHERE p.id_pedido = ? AND p.estado = 'APROBADO'
                  AND pe.estado = 'CANCELADO' AND u.estado = 'ACTIVO'
                  AND u.tipo_registro = 'CUENTA' AND r.nombre = 'CLIENTE'
                ORDER BY p.fecha_pago DESC, p.id_pago DESC
                FOR UPDATE
                """, (rs, fila) -> new ReversionPago(
                rs.getInt("id_pago"), rs.getInt("id_cliente"), rs.getString("codigo_pedido")
        ), idPedido);
        if (pagos.isEmpty()) {
            liberarCuponesDelPedido(idPedido);
            return;
        }
        for (ReversionPago pago : pagos) {
            revertirPagoAprobado(pago, administrador, motivo);
        }
        liberarCuponUsadoDelPedido(idPedido);
    }

    @Transactional
    public void cancelarCanje(Long idCanje, Usuario administrador, String motivo) {
        validarAdministrador(administrador);
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 8) {
            throw new IllegalStateException("Describe el motivo de la cancelación (mínimo 8 caracteres).");
        }
        List<CanjeBloqueado> encontrados = jdbcTemplate.query("""
                SELECT c.id_canje, c.id_usuario, c.id_recompensa,
                       c.puntos_usados, c.estado, r.stock
                FROM club_canjes c JOIN club_recompensas r ON r.id_recompensa = c.id_recompensa
                WHERE c.id_canje = ? FOR UPDATE
                """, (rs, fila) -> new CanjeBloqueado(
                rs.getLong("id_canje"), rs.getInt("id_usuario"),
                rs.getInt("id_recompensa"), rs.getInt("puntos_usados"),
                rs.getString("estado"), rs.getObject("stock", Integer.class)
        ), idCanje);
        CanjeBloqueado canje = encontrados.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No encontramos el canje."));
        if (!List.of("DISPONIBLE", "PENDIENTE_ENTREGA").contains(canje.estado())) {
            throw new IllegalStateException("Solo se pueden cancelar recompensas sin utilizar.");
        }

        asegurarSaldo(canje.idUsuario());
        saldoBloqueado(canje.idUsuario());
        devolverPuntosCanje(canje);
        jdbcTemplate.update("""
                UPDATE club_canjes SET estado = 'CANCELADO'
                WHERE id_canje = ? AND estado IN ('DISPONIBLE', 'PENDIENTE_ENTREGA')
                """, idCanje);
        if (canje.stock() != null) {
            jdbcTemplate.update("UPDATE club_recompensas SET stock = stock + 1 WHERE id_recompensa = ?",
                    canje.idRecompensa());
        }
        jdbcTemplate.update("""
                INSERT INTO club_movimientos
                    (id_usuario, id_canje, id_admin, tipo, puntos, descripcion, referencia_operacion)
                VALUES (?, NULL, ?, 'REVERSO', ?, ?, ?)
                """, canje.idUsuario(), administrador.getIdUsuario(), canje.puntosUsados(),
                "Reverso de canje: " + limitarTexto(normalizarMotivo(motivo), 150), "CANCELACION-CANJE-" + idCanje);
    }

    @Transactional
    public void ajustarPuntos(Usuario cliente, Usuario administrador, int puntos, String motivo) {
        validarCliente(cliente);
        validarAdministrador(administrador);
        if (puntos == 0 || motivo == null || motivo.isBlank()) {
            throw new IllegalStateException("Indica una cantidad distinta de cero y un motivo.");
        }
        asegurarSaldo(cliente.getIdUsuario());
        int saldo = saldoBloqueado(cliente.getIdUsuario());
        String referencia = "AJUSTE-" + UUID.randomUUID().toString().replace("-", "");
        if (puntos < 0) {
            if (puntos == Integer.MIN_VALUE) {
                throw new IllegalStateException("El ajuste supera el límite permitido.");
            }
            if (saldo < Math.abs((long) puntos)) {
                throw new IllegalStateException("El ajuste dejaría el saldo del cliente en negativo.");
            }
            consumirLotes(cliente.getIdUsuario(), Math.abs(puntos), null);
            jdbcTemplate.update("UPDATE club_saldos SET puntos = puntos - ? WHERE id_usuario = ?",
                    Math.abs(puntos), cliente.getIdUsuario());
        } else {
            int aplicado = Math.min(deudaBloqueada(cliente.getIdUsuario()), puntos);
            int acreditado = puntos - aplicado;
            if (acreditado > 0) {
                int dias = Math.max(1, configuracion("VIGENCIA_PUNTOS_DIAS", 365));
                crearLote(cliente.getIdUsuario(), null, referencia, acreditado,
                        LocalDateTime.now().plusDays(dias));
                jdbcTemplate.update("UPDATE club_saldos SET puntos = puntos + ? WHERE id_usuario = ?",
                        acreditado, cliente.getIdUsuario());
            }
            if (aplicado > 0) {
                jdbcTemplate.update("UPDATE club_saldos SET puntos_deuda = puntos_deuda - ? WHERE id_usuario = ?",
                        aplicado, cliente.getIdUsuario());
            }
        }
        jdbcTemplate.update("""
                INSERT INTO club_movimientos
                    (id_usuario, id_admin, tipo, puntos, descripcion, referencia_operacion)
                VALUES (?, ?, 'AJUSTE', ?, ?, ?)
                """, cliente.getIdUsuario(), administrador.getIdUsuario(), puntos,
                motivo.trim(), referencia);
    }

    @Transactional
    public String validarCodigoEnCaja(String codigo, Usuario cajero) {
        if (!esCajero(cajero)) {
            throw new IllegalStateException("No tienes permiso para validar canjes.");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalStateException("Ingresa el código del canje.");
        }
        List<ClubCuponDTO> filas = jdbcTemplate.query("""
                SELECT id_canje, nombre_recompensa, codigo_cupon, estado,
                       puntos_usados, fecha_canje, vence_en, tipo, categoria,
                       valor, compra_minima, condiciones
                FROM club_canjes WHERE codigo_cupon = ? FOR UPDATE
                """, this::mapearCanje, codigo.trim().toUpperCase(Locale.ROOT));
        ClubCuponDTO canje = filas.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("El código no existe."));
        if (!esRecompensaFisica(canje.tipo())) {
            throw new IllegalStateException("Este código se aplica al pedido online, no se valida en caja.");
        }
        if (canje.venceEn() == null || !canje.venceEn().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("El canje venció y no se puede utilizar.");
        }
        if (!List.of("PENDIENTE_ENTREGA", "DISPONIBLE").contains(canje.estado())) {
            throw new IllegalStateException("El código ya fue utilizado, cancelado o reservado.");
        }
        if (canje.condiciones() != null
                && canje.condiciones().toLowerCase(Locale.ROOT).contains("lunes a viernes")
                && LocalDate.now(ZoneId.of("America/Lima")).getDayOfWeek().getValue() > 5) {
            throw new IllegalStateException("Este canje solo esta disponible de lunes a viernes.");
        }
        int actualizado = jdbcTemplate.update("""
                UPDATE club_canjes
                SET estado = 'USADO', usado_en = NOW(), id_usuario_validador = ?
                WHERE id_canje = ? AND estado IN ('PENDIENTE_ENTREGA', 'DISPONIBLE')
                """, cajero.getIdUsuario(), canje.idCanje());
        if (actualizado != 1) throw new IllegalStateException("El canje ya fue validado por otra caja.");
        return canje.nombreRecompensa();
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Lima")
    @Transactional
    public void expirarPuntosVencidos() {
        List<Integer> usuarios = jdbcTemplate.queryForList("""
                SELECT DISTINCT id_usuario FROM club_lotes_puntos
                WHERE puntos_disponibles > 0 AND vence_en IS NOT NULL AND vence_en <= NOW()
                """, Integer.class);
        usuarios.forEach(this::expirarPuntosUsuario);
    }

    @Scheduled(cron = "0 20 3 * * *", zone = "America/Lima")
    @Transactional
    public void expirarCanjesVencidos() {
        jdbcTemplate.update("""
                UPDATE club_canjes SET estado = 'VENCIDO'
                WHERE estado IN ('DISPONIBLE', 'PENDIENTE_ENTREGA') AND vence_en <= NOW()
                """);
    }

    @Transactional(readOnly = true)
    public int obtenerConfiguracion(String clave, int predeterminado) {
        return configuracion(clave, predeterminado);
    }

    @Transactional(readOnly = true)
    public ClubNivelDTO obtenerNivelFidelizacion(Usuario cliente) {
        validarCliente(cliente);
        return nivelFidelizacion(puntosAcumuladosEnAnio(cliente.getIdUsuario()));
    }

    private long puntosAcumuladosEnAnio(Integer idUsuario) {
        LocalDate inicioAnio = LocalDate.now(ZoneId.of("America/Lima")).withDayOfYear(1);
        Timestamp desde = Timestamp.valueOf(inicioAnio.atStartOfDay());
        Timestamp hasta = Timestamp.valueOf(inicioAnio.plusYears(1).atStartOfDay());
        Long puntos = jdbcTemplate.queryForObject("""
                SELECT COALESCE((
                    SELECT SUM(m.puntos)
                    FROM club_movimientos m
                    WHERE m.id_usuario = ?
                      AND m.creado_en >= ? AND m.creado_en < ?
                      AND (
                          (m.tipo = 'ACUMULACION' AND m.id_pago IS NOT NULL)
                          OR (m.tipo = 'BONIFICACION' AND (
                              m.referencia_operacion LIKE 'BONO-PAGO-%'
                              OR m.referencia_operacion LIKE 'BONO-NIVEL-PAGO-%'
                          ))
                      )
                ), 0) + COALESCE((
                    SELECT SUM(reverso.puntos)
                    FROM club_movimientos reverso
                    JOIN club_movimientos origen
                      ON origen.id_usuario = reverso.id_usuario
                     AND origen.id_pago = CAST(
                         SUBSTRING_INDEX(reverso.referencia_operacion, '-', -1) AS UNSIGNED
                     )
                     AND origen.tipo = 'ACUMULACION'
                    WHERE reverso.id_usuario = ?
                      AND reverso.tipo = 'REVERSO'
                      AND reverso.referencia_operacion LIKE 'REVERSO-PAGO-%'
                      AND origen.creado_en >= ? AND origen.creado_en < ?
                ), 0)
                """, Long.class, idUsuario, desde, hasta, idUsuario, desde, hasta);
        return Math.max(0L, puntos == null ? 0L : puntos);
    }

    private ClubNivelDTO nivelFidelizacion(long puntosAcumulados) {
        int puntos = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, puntosAcumulados));
        if (puntos >= 1_500) {
            return new ClubNivelDTO(
                    "FAMILIA", "Familia Rinconcito", puntos, 20,
                    "20% adicional en puntos y promociones exclusivas",
                    null, null, 100
            );
        }
        if (puntos >= 500) {
            int progreso = (int) Math.min(100L, (long) (puntos - 500) * 100 / 1_000);
            return new ClubNivelDTO(
                    "AMIGO", "Amigo del Rinconcito", puntos, 10,
                    "10% adicional en puntos",
                    "Familia Rinconcito", 1_500, progreso
            );
        }
        int progreso = (int) Math.min(100L, (long) puntos * 100 / 500);
        return new ClubNivelDTO(
                "VISITANTE", "Visitante", puntos, 0,
                "1 punto por cada S/ 1 consumido",
                "Amigo del Rinconcito", 500, progreso
        );
    }

    private void revertirPagoAprobado(ReversionPago pago, Usuario administrador, String motivo) {
        String referencia = "REVERSO-PAGO-" + pago.idPago();
        Integer existe = jdbcTemplate.query("""
                SELECT COUNT(*) FROM club_movimientos WHERE referencia_operacion = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, referencia);
        if (existe != null && existe > 0) return;

        Integer puntosOriginales = jdbcTemplate.query("""
                SELECT COALESCE(SUM(puntos), 0)
                FROM club_movimientos
                WHERE id_usuario = ? AND referencia_operacion IN (?, ?, ?)
                  AND tipo IN ('ACUMULACION', 'BONIFICACION')
                """, rs -> rs.next() ? rs.getInt(1) : 0,
                pago.idUsuario(), "PAGO-" + pago.idPago(), "BONO-PAGO-" + pago.idPago(),
                "BONO-NIVEL-PAGO-" + pago.idPago());
        if (puntosOriginales == null || puntosOriginales <= 0) return;

        asegurarSaldo(pago.idUsuario());
        int saldo = saldoBloqueado(pago.idUsuario());
        int aplicadoADeuda = jdbcTemplate.query("""
                SELECT COALESCE(SUM(-puntos), 0)
                FROM club_movimientos
                WHERE id_usuario = ? AND referencia_operacion = ? AND tipo = 'REVERSO'
                """, rs -> rs.next() ? rs.getInt(1) : 0,
                pago.idUsuario(), "COMPENSACION-PAGO-" + pago.idPago());
        int puntosAcreditados = Math.max(0, puntosOriginales - aplicadoADeuda);
        int retirados = retirarDelLotePago(pago.idPago(), puntosAcreditados);
        int pendientes = puntosAcreditados - retirados;
        int deudaNueva = aplicadoADeuda + pendientes;
        int otrosLotes = Math.min(pendientes, Math.max(0, saldo - retirados));
        if (otrosLotes > 0) {
            consumirLotesSinCanje(pago.idUsuario(), otrosLotes);
            retirados += otrosLotes;
            deudaNueva -= otrosLotes;
        }
        jdbcTemplate.update("""
                UPDATE club_saldos
                SET puntos = GREATEST(0, puntos - ?), puntos_deuda = puntos_deuda + ?
                WHERE id_usuario = ?
                """, retirados, deudaNueva, pago.idUsuario());
        jdbcTemplate.update("""
                INSERT INTO club_movimientos
                    (id_usuario, id_admin, tipo, puntos, descripcion, referencia_operacion)
                VALUES (?, ?, 'REVERSO', ?, ?, ?)
                """, pago.idUsuario(), administrador.getIdUsuario(),
                -puntosOriginales, "Reverso del pedido " + pago.codigoPedido()
                        + ": " + limitarTexto(normalizarMotivo(motivo), 130), referencia);
    }

    private int retirarDelLotePago(Integer idPago, int maximo) {
        if (maximo <= 0) return 0;
        List<Lote> lotes = jdbcTemplate.query("""
                SELECT id_lote, id_usuario, puntos_disponibles
                FROM club_lotes_puntos WHERE id_pago = ? AND puntos_disponibles > 0
                FOR UPDATE
                """, (rs, fila) -> new Lote(
                rs.getLong("id_lote"), rs.getInt("id_usuario"), rs.getInt("puntos_disponibles")
        ), idPago);
        int retirados = 0;
        for (Lote lote : lotes) {
            int cantidad = Math.min(maximo - retirados, lote.disponibles());
            if (cantidad <= 0) break;
            jdbcTemplate.update("UPDATE club_lotes_puntos SET puntos_disponibles = puntos_disponibles - ? WHERE id_lote = ?",
                    cantidad, lote.idLote());
            retirados += cantidad;
        }
        return retirados;
    }

    private void liberarCuponUsadoDelPedido(Integer idPedido) {
        jdbcTemplate.update("""
                UPDATE club_canjes
                SET estado = IF(vence_en > NOW(), IF(tipo IN ('PRODUCTO_CARTA', 'PRODUCTO_MERCH'), 'PENDIENTE_ENTREGA', 'DISPONIBLE'), 'VENCIDO'),
                    id_pedido = NULL, usado_en = NULL
                WHERE id_pedido = ? AND estado = 'USADO'
                """, idPedido);
    }

    private void devolverPuntosCanje(CanjeBloqueado canje) {
        List<ConsumoLote> consumos = jdbcTemplate.query("""
                SELECT c.id_lote, c.puntos, l.vence_en
                FROM club_consumos_lotes c
                JOIN club_lotes_puntos l ON l.id_lote = c.id_lote
                WHERE c.id_canje = ?
                FOR UPDATE
                """, (rs, fila) -> new ConsumoLote(
                rs.getLong("id_lote"), rs.getInt("puntos"), fechaNullable(rs, "vence_en")
        ), canje.idCanje());

        int devueltos = 0;
        for (ConsumoLote consumo : consumos) {
            if (consumo.venceEn() == null || consumo.venceEn().isAfter(LocalDateTime.now())) {
                jdbcTemplate.update("UPDATE club_lotes_puntos SET puntos_disponibles = puntos_disponibles + ? WHERE id_lote = ?",
                        consumo.puntos(), consumo.idLote());
                devueltos += consumo.puntos();
            } else {
                crearLote(canje.idUsuario(), null,
                        "REVERSO-CANJE-" + canje.idCanje() + "-" + consumo.idLote(),
                        consumo.puntos(), LocalDateTime.now().plusDays(configuracion("VIGENCIA_PUNTOS_DIAS", 365)));
                devueltos += consumo.puntos();
            }
        }
        if (devueltos == 0) {
            int dias = Math.max(1, configuracion("VIGENCIA_PUNTOS_DIAS", 365));
            crearLote(canje.idUsuario(), null, "REVERSO-CANJE-" + canje.idCanje(),
                    canje.puntosUsados(), LocalDateTime.now().plusDays(dias));
            devueltos = canje.puntosUsados();
        }
        jdbcTemplate.update("UPDATE club_saldos SET puntos = puntos + ? WHERE id_usuario = ?",
                devueltos, canje.idUsuario());
    }

    private void consumirLotes(Integer idUsuario, int puntos, Long idCanje) {
        List<Lote> lotes = jdbcTemplate.query("""
                SELECT id_lote, id_usuario, puntos_disponibles
                FROM club_lotes_puntos
                WHERE id_usuario = ? AND puntos_disponibles > 0
                ORDER BY (vence_en IS NULL), vence_en, creado_en, id_lote
                FOR UPDATE
                """, (rs, fila) -> new Lote(
                rs.getLong("id_lote"), rs.getInt("id_usuario"), rs.getInt("puntos_disponibles")
        ), idUsuario);
        int restante = puntos;
        for (Lote lote : lotes) {
            int usado = Math.min(restante, lote.disponibles());
            if (usado <= 0) continue;
            jdbcTemplate.update("""
                    UPDATE club_lotes_puntos
                    SET puntos_disponibles = puntos_disponibles - ?
                    WHERE id_lote = ? AND puntos_disponibles >= ?
                    """, usado, lote.idLote(), usado);
            if (idCanje != null) {
                jdbcTemplate.update("""
                        INSERT INTO club_consumos_lotes (id_canje, id_lote, puntos)
                        VALUES (?, ?, ?)
                        """, idCanje, lote.idLote(), usado);
            }
            restante -= usado;
            if (restante == 0) break;
        }
        if (restante > 0) {
            throw new IllegalStateException("No se pudieron reservar los puntos. Actualiza el saldo e inténtalo otra vez.");
        }
    }

    private void consumirLotesSinCanje(Integer idUsuario, int puntos) {
        List<Lote> lotes = jdbcTemplate.query("""
                SELECT id_lote, id_usuario, puntos_disponibles
                FROM club_lotes_puntos
                WHERE id_usuario = ? AND puntos_disponibles > 0
                ORDER BY (vence_en IS NULL), vence_en, creado_en, id_lote
                FOR UPDATE
                """, (rs, fila) -> new Lote(
                rs.getLong("id_lote"), rs.getInt("id_usuario"), rs.getInt("puntos_disponibles")
        ), idUsuario);
        int restante = puntos;
        for (Lote lote : lotes) {
            int usado = Math.min(restante, lote.disponibles());
            if (usado <= 0) continue;
            jdbcTemplate.update("UPDATE club_lotes_puntos SET puntos_disponibles = puntos_disponibles - ? WHERE id_lote = ?",
                    usado, lote.idLote());
            restante -= usado;
            if (restante == 0) break;
        }
        if (restante > 0) throw new IllegalStateException("No hay lotes suficientes para el ajuste solicitado.");
    }

    private void expirarPuntosUsuario(Integer idUsuario) {
        asegurarSaldo(idUsuario);
        int saldo = saldoBloqueado(idUsuario);
        List<Lote> lotes = jdbcTemplate.query("""
                SELECT id_lote, id_usuario, puntos_disponibles
                FROM club_lotes_puntos
                WHERE id_usuario = ? AND puntos_disponibles > 0
                  AND vence_en IS NOT NULL AND vence_en <= NOW()
                ORDER BY vence_en, id_lote FOR UPDATE
                """, (rs, fila) -> new Lote(
                rs.getLong("id_lote"), rs.getInt("id_usuario"), rs.getInt("puntos_disponibles")
        ), idUsuario);
        for (Lote lote : lotes) {
            String referencia = "VENCIMIENTO-LOTE-" + lote.idLote();
            int insertado = jdbcTemplate.update("""
                    INSERT IGNORE INTO club_movimientos
                        (id_usuario, id_lote, tipo, puntos, descripcion, referencia_operacion)
                    VALUES (?, ?, 'VENCIMIENTO', ?, 'Vencimiento de puntos', ?)
                    """, idUsuario, lote.idLote(), -lote.disponibles(), referencia);
            if (insertado == 1) {
                jdbcTemplate.update("UPDATE club_lotes_puntos SET puntos_disponibles = 0 WHERE id_lote = ?",
                        lote.idLote());
                jdbcTemplate.update("UPDATE club_saldos SET puntos = GREATEST(0, puntos - ?) WHERE id_usuario = ?",
                        lote.disponibles(), idUsuario);
            }
        }
    }

    private Long crearLote(
            Integer idUsuario,
            Integer idPago,
            String referencia,
            int puntos,
            LocalDateTime venceEn
    ) {
        if (puntos <= 0) return null;
        jdbcTemplate.update("""
                INSERT INTO club_lotes_puntos
                    (id_usuario, id_pago, codigo_origen, puntos_originales,
                     puntos_disponibles, creado_en, vence_en)
                VALUES (?, ?, ?, ?, ?, NOW(), ?)
                """, idUsuario, idPago, referencia, puntos, puntos,
                java.sql.Timestamp.valueOf(venceEn));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private CampanaActiva obtenerCampanaActiva() {
        List<CampanaActiva> activas = jdbcTemplate.query("""
                SELECT id_campana, nombre, porcentaje_bonus
                FROM club_campanas
                WHERE activa = 1 AND fecha_inicio <= NOW() AND fecha_fin >= NOW()
                ORDER BY porcentaje_bonus DESC, id_campana DESC LIMIT 1
                """, (rs, fila) -> new CampanaActiva(
                rs.getLong("id_campana"), rs.getString("nombre"), rs.getInt("porcentaje_bonus")
        ));
        return activas.stream().findFirst().orElse(null);
    }

    private CalculoPuntos calcularPuntos(int montoElegible, long puntosAnuales) {
        int base = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, (long) montoElegible)
                        * Math.max(0, configuracion("PUNTOS_POR_SOL", puntosPorSolPorDefecto)));
        if (base <= 0) return new CalculoPuntos(0, 0, 0, null);
        CampanaActiva campana = obtenerCampanaActiva();
        int bonoCampana = campana == null ? 0
                : (int) Math.min((long) Integer.MAX_VALUE - base,
                        (long) base * campana.porcentajeBonus() / 100);
        ClubNivelDTO nivel = nivelFidelizacion(puntosAnuales);
        long espacioRestante = Math.max(0L, (long) Integer.MAX_VALUE - base - bonoCampana);
        int bonoNivel = (int) Math.min(espacioRestante,
                (long) base * nivel.porcentajeBonus() / 100);
        return new CalculoPuntos(base, bonoNivel, bonoCampana,
                campana == null ? null : campana.nombre());
    }

    private int montoElegiblePedido(Pedido pedido) {
        int subtotal = pedido.getSubtotal() == null ? 0 : pedido.getSubtotal();
        int descuento = pedido.getDescuentoClub() == null ? 0 : pedido.getDescuentoClub();
        String tipoCupon = pedido.getIdPedido() == null ? null : jdbcTemplate.query("""
                SELECT tipo FROM club_canjes WHERE id_pedido = ?
                """, rs -> rs.next() ? rs.getString(1) : null, pedido.getIdPedido());
        int descuentoProductos = "DELIVERY-GRATIS".equals(tipoCupon)
                || "DELIVERY_GRATIS".equals(tipoCupon) ? 0 : descuento;
        return (int) Math.max(0L, (long) subtotal - descuentoProductos);
    }

    private int valorSaldo(Integer idUsuario, String columna) {
        if (!List.of("puntos", "puntos_deuda").contains(columna)) {
            throw new IllegalArgumentException("Columna de saldo no permitida.");
        }
        List<Integer> saldos = jdbcTemplate.queryForList(
                "SELECT " + columna + " FROM club_saldos WHERE id_usuario = ?", Integer.class, idUsuario);
        return saldos.isEmpty() ? 0 : saldos.getFirst();
    }

    private ClubPagoResumenDTO buscarResumenPago(Integer idPago) {
        if (idPago == null) return null;
        List<ClubPagoResumenDTO> filas = jdbcTemplate.query("""
                SELECT CONCAT_WS(' ', u.nombres, u.apellidos) AS cliente_nombre,
                       r.nivel_codigo, r.nivel_nombre, r.puntos_anuales_antes,
                       r.saldo_anterior, r.deuda_anterior, r.puntos_base,
                       r.puntos_bonus_nivel, r.puntos_bonus_campana,
                       r.campana_nombre, r.puntos_totales, r.puntos_aplicados_deuda,
                       r.puntos_acreditados, r.saldo_nuevo, r.deuda_nueva,
                       r.nuevo_nivel_codigo, r.nuevo_nivel_nombre
                FROM club_pago_resumen r
                JOIN usuarios u ON u.id_usuario = r.id_usuario
                WHERE r.id_pago = ?
                """, (rs, fila) -> new ClubPagoResumenDTO(
                rs.getString("cliente_nombre"), rs.getString("nivel_codigo"),
                rs.getString("nivel_nombre"), rs.getInt("puntos_anuales_antes"),
                rs.getInt("saldo_anterior"), rs.getInt("deuda_anterior"),
                rs.getInt("puntos_base"), rs.getInt("puntos_bonus_nivel"),
                rs.getInt("puntos_bonus_campana"), rs.getString("campana_nombre"),
                rs.getInt("puntos_totales"), rs.getInt("puntos_aplicados_deuda"),
                rs.getInt("puntos_acreditados"), rs.getInt("saldo_nuevo"),
                rs.getInt("deuda_nueva"), rs.getString("nuevo_nivel_codigo"),
                rs.getString("nuevo_nivel_nombre")
        ), idPago);
        return filas.stream().findFirst().orElse(null);
    }

    private CanjeCaja cargarCanjeCajaBloqueado(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalStateException("Ingresa el código del canje.");
        }
        List<CanjeCaja> filas = jdbcTemplate.query("""
                SELECT c.id_canje, c.id_usuario, c.nombre_recompensa, c.codigo_cupon,
                       c.estado, c.vence_en, c.tipo, c.condiciones, r.codigo AS codigo_recompensa
                FROM club_canjes c
                JOIN club_recompensas r ON r.id_recompensa = c.id_recompensa
                WHERE c.codigo_cupon = ? FOR UPDATE
                """, (rs, fila) -> new CanjeCaja(
                rs.getLong("id_canje"), rs.getInt("id_usuario"),
                rs.getString("nombre_recompensa"), rs.getString("codigo_cupon"),
                rs.getString("estado"), fechaNullable(rs, "vence_en"),
                rs.getString("tipo"), rs.getString("condiciones"), rs.getString("codigo_recompensa")
        ), codigo.trim().toUpperCase(Locale.ROOT));
        return filas.stream().findFirst().orElseThrow(() -> new IllegalStateException("El código no existe."));
    }

    private String validarCanjeCaja(CanjeCaja canje, Usuario cliente) {
        if (canje == null) return "No encontramos ese código de recompensa.";
        if (!esRecompensaFisica(canje.tipo())) {
            return "Este código es para pedidos online; no se puede aplicar a un consumo presencial.";
        }
        if (cliente != null && !cliente.getIdUsuario().equals(canje.idUsuario())) {
            return "Ese código no pertenece al cliente identificado.";
        }
        if (canje.venceEn() == null || !canje.venceEn().isAfter(LocalDateTime.now())) {
            return "El canje venció y no se puede utilizar.";
        }
        if (!List.of("PENDIENTE_ENTREGA", "DISPONIBLE").contains(canje.estado())) {
            return "El código ya fue utilizado, cancelado o reservado.";
        }
        if (requiereCanjeEntreSemana(canje.codigoRecompensa())
                && LocalDate.now(ZoneId.of("America/Lima")).getDayOfWeek().getValue() > 5) {
            return "Este canje solo está disponible de lunes a viernes.";
        }
        return null;
    }

    private String consumirCanjeEnCaja(CanjeCaja canje, Usuario cajero) {
        if (!esCajero(cajero)) throw new IllegalStateException("No tienes permiso para validar canjes.");
        String mensaje = validarCanjeCaja(canje, null);
        if (mensaje != null) throw new IllegalStateException(mensaje);
        int actualizado = jdbcTemplate.update("""
                UPDATE club_canjes
                SET estado = 'USADO', usado_en = NOW(), id_usuario_validador = ?
                WHERE id_canje = ? AND estado IN ('PENDIENTE_ENTREGA', 'DISPONIBLE')
                """, cajero.getIdUsuario(), canje.idCanje());
        if (actualizado != 1) throw new IllegalStateException("El canje ya fue validado por otra caja.");
        return canje.nombre();
    }

    private int configuracion(String clave, int predeterminado) {
        List<String> valores = jdbcTemplate.query("""
                SELECT valor FROM club_configuracion WHERE clave = ?
                """, (rs, fila) -> rs.getString("valor"), clave);
        if (valores.isEmpty()) return predeterminado;
        try {
            return Integer.parseInt(valores.getFirst());
        } catch (NumberFormatException e) {
            return predeterminado;
        }
    }

    private int saldoBloqueado(Integer idUsuario) {
        Integer saldo = jdbcTemplate.queryForObject("""
                SELECT puntos FROM club_saldos WHERE id_usuario = ? FOR UPDATE
                """, Integer.class, idUsuario);
        return saldo == null ? 0 : saldo;
    }

    private int deudaBloqueada(Integer idUsuario) {
        Integer deuda = jdbcTemplate.queryForObject("""
                SELECT puntos_deuda FROM club_saldos WHERE id_usuario = ? FOR UPDATE
                """, Integer.class, idUsuario);
        return deuda == null ? 0 : deuda;
    }

    private void asegurarSaldo(Integer idUsuario) {
        jdbcTemplate.update("INSERT IGNORE INTO club_saldos (id_usuario, puntos, puntos_deuda) VALUES (?, 0, 0)", idUsuario);
    }

    private ClubRecompensaDTO mapearRecompensa(ResultSet rs, int fila, int saldo) throws SQLException {
        Integer idProducto = rs.getObject("id_producto", Integer.class);
        Integer stock = rs.getObject("stock", Integer.class);
        Integer puntos = rs.getObject("costo_puntos", Integer.class);
        LocalDateTime desde = fechaNullable(rs, "disponible_desde");
        LocalDateTime hasta = fechaNullable(rs, "disponible_hasta");
        String estado = "DISPONIBLE";
        LocalDateTime ahora = LocalDateTime.now();
        if (stock != null && stock <= 0) estado = "AGOTADA";
        else if (idProducto != null && (!"ACTIVO".equals(rs.getString("estado_producto"))
                || !rs.getBoolean("producto_disponible"))) estado = "NO_DISPONIBLE_EN_CARTA";
        else if (desde != null && desde.isAfter(ahora)) estado = "PROXIMAMENTE";
        else if (hasta != null && hasta.isBefore(ahora)) estado = "VENCIDA";
        else if ("DELIVERY_GRATIS".equals(rs.getString("tipo")) && costoDeliveryConfigurado <= 0) {
            estado = "CONFIGURACION_PENDIENTE";
        }
        int faltantes = Math.max(0, (puntos == null ? 0 : puntos) - saldo);
        int porcentaje = puntos == null || puntos <= 0 ? 0
                : (int) Math.min(100L, (long) saldo * 100 / puntos);
        boolean canjeable = "DISPONIBLE".equals(estado) && faltantes == 0;
        return new ClubRecompensaDTO(
                rs.getInt("id_recompensa"), rs.getString("codigo"),
                rs.getString("nombre"), rs.getString("descripcion"), puntos,
                rs.getString("tipo"), rs.getString("categoria"), rs.getInt("valor"),
                rs.getInt("compra_minima"), rs.getString("condiciones"),
                rs.getString("imagen"), stock, rs.getInt("vigencia_dias"),
                idProducto, rs.getObject("precio_referencia", Integer.class), desde, hasta, canjeable, estado, faltantes, porcentaje
        );
    }

    private ClubCuponDTO mapearCanje(ResultSet rs, int fila) throws SQLException {
        return new ClubCuponDTO(
                rs.getLong("id_canje"), rs.getString("nombre_recompensa"),
                rs.getString("codigo_cupon"), rs.getString("estado"),
                rs.getInt("puntos_usados"), fechaNullable(rs, "fecha_canje"),
                fechaNullable(rs, "vence_en"), rs.getString("tipo"),
                rs.getString("categoria"), rs.getInt("valor"),
                rs.getInt("compra_minima"), rs.getString("condiciones")
        );
    }

    private LocalDateTime fechaNullable(ResultSet rs, String columna) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(columna);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String mensajeNoDisponible(ClubRecompensaDTO recompensa) {
        return switch (recompensa.estadoDisponibilidad()) {
            case "AGOTADA" -> "Esta recompensa está agotada.";
            case "PROXIMAMENTE" -> "Esta recompensa todavía no está disponible.";
            case "VENCIDA" -> "La vigencia de esta recompensa terminó.";
            case "NO_DISPONIBLE_EN_CARTA" -> "Este producto no está disponible actualmente en la carta.";
            case "CONFIGURACION_PENDIENTE" -> "El costo del delivery aún no está configurado.";
            default -> "Esta recompensa no está disponible para canje.";
        };
    }

    private String nuevoCodigoCupon() {
        return "RIN-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private boolean requiereCanjeEntreSemana(String codigo) {
        return "CUY-CHACTADO".equals(codigo) || "PACHAMANCA-ESPECIAL".equals(codigo);
    }

    private boolean esRecompensaFisica(String tipo) {
        return List.of("PRODUCTO_CARTA", "PRODUCTO_MERCH", "EXPERIENCIA", "PRODUCTO")
                .contains(tipo);
    }

    private boolean esClienteElegible(Usuario cliente) {
        return cliente != null && cliente.getIdUsuario() != null
                && cliente.getEstado() == Usuario.EstadoUsuario.ACTIVO
                && cliente.getTipoRegistro() == Usuario.TipoRegistro.CUENTA
                && cliente.getRol() != null
                && "CLIENTE".equals(cliente.getRol().getNombre());
    }

    private String nombreCompleto(Usuario cliente) {
        return ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " "
                + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim();
    }

    private void validarCliente(Usuario cliente) {
        if (!esClienteElegible(cliente)) {
            throw new IllegalStateException("El Club está disponible para cuentas activas de cliente.");
        }
    }

    private boolean esCajero(Usuario usuario) {
        return usuario != null && usuario.getIdUsuario() != null
                && usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO
                && usuario.getRol() != null
                && List.of("CAJA", "ADMINISTRADOR").contains(usuario.getRol().getNombre());
    }

    private void validarAdministrador(Usuario administrador) {
        if (administrador == null || administrador.getEstado() != Usuario.EstadoUsuario.ACTIVO
                || administrador.getRol() == null
                || !"ADMINISTRADOR".equals(administrador.getRol().getNombre())) {
            throw new IllegalStateException("Se requiere un administrador activo para esta operación.");
        }
    }

    private String normalizarMotivo(String motivo) {
        return motivo == null || motivo.isBlank() ? "anulación del pedido" : motivo.trim();
    }

    private String limitarTexto(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private record CalculoPuntos(int base, int bonoNivel, int bonoCampana, String campanaNombre) {
        int total() {
            return base + bonoNivel + bonoCampana;
        }
    }
    private record CanjeCaja(Long idCanje, Integer idUsuario, String nombre, String codigoCupon,
                             String estado, LocalDateTime venceEn, String tipo, String condiciones,
                             String codigoRecompensa) { }
    private record Lote(Long idLote, Integer idUsuario, int disponibles) { }
    private record ConsumoLote(Long idLote, int puntos, LocalDateTime venceEn) { }
    private record CampanaActiva(Long idCampana, String nombre, int porcentajeBonus) { }
    private record ReversionPago(Integer idPago, Integer idUsuario, String codigoPedido) { }
    private record CanjeBloqueado(Long idCanje, Integer idUsuario, Integer idRecompensa,
                                  int puntosUsados, String estado, Integer stock) { }
}
