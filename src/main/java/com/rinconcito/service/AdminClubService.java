package com.rinconcito.service;

import com.rinconcito.dto.ClubCampanaAdminDTO;
import com.rinconcito.dto.ClubCampanaFormDTO;
import com.rinconcito.dto.ClubCanjeAdminDTO;
import com.rinconcito.dto.ClubConfiguracionFormDTO;
import com.rinconcito.dto.ClubProductoDTO;
import com.rinconcito.dto.ClubRecompensaAdminDTO;
import com.rinconcito.dto.ClubRecompensaFormDTO;
import com.rinconcito.entity.Usuario;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AdminClubService {

    private static final Set<String> TIPOS = Set.of(
            "DESCUENTO_FIJO", "DESCUENTO_PORCENTAJE", "DELIVERY_GRATIS",
            "PRODUCTO_CARTA", "PRODUCTO_MERCH", "EXPERIENCIA"
    );
    private static final Set<String> CATEGORIAS = Set.of(
            "PLATOS_TIPICOS", "BEBIDAS_POSTRES", "DESCUENTOS",
            "EXPERIENCIAS_FAMILIARES", "MERCHANDISING"
    );

    private final JdbcTemplate jdbc;

    public AdminClubService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ClubRecompensaAdminDTO> listarRecompensas() {
        return jdbc.query("""
                SELECT r.id_recompensa, r.codigo, r.nombre, r.tipo, r.categoria,
                       r.costo_puntos, r.stock, r.activo, r.disponible_desde,
                       r.disponible_hasta, COUNT(c.id_canje) AS total_canjes
                FROM club_recompensas r
                LEFT JOIN club_canjes c ON c.id_recompensa = r.id_recompensa
                GROUP BY r.id_recompensa
                ORDER BY r.activo DESC, r.categoria, r.costo_puntos, r.nombre
                """, (rs, fila) -> new ClubRecompensaAdminDTO(
                rs.getInt("id_recompensa"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("tipo"), rs.getString("categoria"), rs.getInt("costo_puntos"),
                rs.getObject("stock", Integer.class), rs.getBoolean("activo"),
                fecha(rs.getTimestamp("disponible_desde")),
                fecha(rs.getTimestamp("disponible_hasta")), rs.getLong("total_canjes")
        ));
    }

    @Transactional(readOnly = true)
    public ClubRecompensaFormDTO obtenerRecompensa(Integer id) {
        return jdbc.query("""
                SELECT id_recompensa, codigo, nombre, descripcion, tipo, categoria,
                       costo_puntos, valor, compra_minima, condiciones, imagen, stock,
                       vigencia_dias, id_producto, disponible_desde, disponible_hasta, activo
                FROM club_recompensas WHERE id_recompensa = ?
                """, rs -> {
            if (!rs.next()) throw new IllegalStateException("No encontramos esa recompensa.");
            ClubRecompensaFormDTO form = new ClubRecompensaFormDTO();
            form.setIdRecompensa(rs.getInt("id_recompensa"));
            form.setCodigo(rs.getString("codigo"));
            form.setNombre(rs.getString("nombre"));
            form.setDescripcion(rs.getString("descripcion"));
            form.setTipo(rs.getString("tipo"));
            form.setCategoria(rs.getString("categoria"));
            form.setCostoPuntos(rs.getInt("costo_puntos"));
            form.setValor(rs.getInt("valor"));
            form.setCompraMinima(rs.getInt("compra_minima"));
            form.setCondiciones(rs.getString("condiciones"));
            form.setImagen(rs.getString("imagen"));
            form.setStock(rs.getObject("stock", Integer.class));
            form.setVigenciaDias(rs.getInt("vigencia_dias"));
            form.setIdProducto(rs.getObject("id_producto", Integer.class));
            Timestamp desde = rs.getTimestamp("disponible_desde");
            Timestamp hasta = rs.getTimestamp("disponible_hasta");
            form.setDisponibleDesde(desde == null ? null : desde.toLocalDateTime().toLocalDate());
            form.setDisponibleHasta(hasta == null ? null : hasta.toLocalDateTime().toLocalDate());
            form.setActivo(rs.getBoolean("activo"));
            return form;
        }, id);
    }

    @Transactional
    public void guardarRecompensa(ClubRecompensaFormDTO form, Usuario admin) {
        validarRecompensa(form);
        String codigo = form.getCodigo().trim().toUpperCase();
        LocalDateTime desde = form.getDisponibleDesde() == null
                ? null : form.getDisponibleDesde().atStartOfDay();
        LocalDateTime hasta = form.getDisponibleHasta() == null
                ? null : form.getDisponibleHasta().atTime(23, 59, 59);
        Integer producto = "PRODUCTO_CARTA".equals(form.getTipo()) ? form.getIdProducto() : null;
        try {
            if (form.getIdRecompensa() == null) {
                jdbc.update("""
                        INSERT INTO club_recompensas
                            (codigo, nombre, descripcion, costo_puntos, tipo, valor,
                             compra_minima, imagen, categoria, condiciones, vigencia_dias,
                             id_producto, disponible_desde, disponible_hasta, activo, stock)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, codigo, form.getNombre().trim(), form.getDescripcion().trim(),
                        form.getCostoPuntos(), form.getTipo(), form.getValor(), form.getCompraMinima(),
                        normalizar(form.getImagen()), form.getCategoria(), normalizar(form.getCondiciones()),
                        form.getVigenciaDias(), producto, desde, hasta, form.isActivo(), form.getStock());
            } else {
                int filas = jdbc.update("""
                        UPDATE club_recompensas SET codigo = ?, nombre = ?, descripcion = ?,
                            costo_puntos = ?, tipo = ?, valor = ?, compra_minima = ?, imagen = ?,
                            categoria = ?, condiciones = ?, vigencia_dias = ?, id_producto = ?,
                            disponible_desde = ?, disponible_hasta = ?, activo = ?, stock = ?
                        WHERE id_recompensa = ?
                        """, codigo, form.getNombre().trim(), form.getDescripcion().trim(),
                        form.getCostoPuntos(), form.getTipo(), form.getValor(), form.getCompraMinima(),
                        normalizar(form.getImagen()), form.getCategoria(), normalizar(form.getCondiciones()),
                        form.getVigenciaDias(), producto, desde, hasta, form.isActivo(), form.getStock(),
                        form.getIdRecompensa());
                if (filas != 1) throw new IllegalStateException("No encontramos esa recompensa para editar.");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Ya existe una recompensa con ese código.");
        }
    }

    @Transactional
    public void guardarConfiguracion(ClubConfiguracionFormDTO form, Usuario admin) {
        guardarValor("PUNTOS_POR_SOL", form.getPuntosPorSol(), admin);
        guardarValor("VIGENCIA_PUNTOS_DIAS", form.getVigenciaPuntosDias(), admin);
        guardarValor("VIGENCIA_RECOMPENSAS_DIAS", form.getVigenciaRecompensasDias(), admin);
    }

    @Transactional(readOnly = true)
    public ClubConfiguracionFormDTO obtenerConfiguracion() {
        ClubConfiguracionFormDTO form = new ClubConfiguracionFormDTO();
        form.setPuntosPorSol(valorConfig("PUNTOS_POR_SOL", 1));
        form.setVigenciaPuntosDias(valorConfig("VIGENCIA_PUNTOS_DIAS", 365));
        form.setVigenciaRecompensasDias(valorConfig("VIGENCIA_RECOMPENSAS_DIAS", 90));
        return form;
    }

    @Transactional
    public void guardarCampana(ClubCampanaFormDTO form, Usuario admin) {
        if (form.getFechaInicio() == null || form.getFechaFin() == null
                || form.getFechaFin().isBefore(form.getFechaInicio())) {
            throw new IllegalStateException("Indica un rango de fechas válido para la campaña.");
        }
        LocalDateTime inicio = form.getFechaInicio().atStartOfDay();
        LocalDateTime fin = form.getFechaFin().atTime(23, 59, 59);
        if (form.getIdCampana() == null) {
            jdbc.update("""
                    INSERT INTO club_campanas
                        (nombre, porcentaje_bonus, fecha_inicio, fecha_fin, activa, id_admin)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, form.getNombre().trim(), form.getPorcentajeBonus(), inicio, fin,
                    form.isActiva(), admin.getIdUsuario());
        } else {
            int filas = jdbc.update("""
                    UPDATE club_campanas SET nombre = ?, porcentaje_bonus = ?,
                        fecha_inicio = ?, fecha_fin = ?, activa = ?, id_admin = ?
                    WHERE id_campana = ?
                    """, form.getNombre().trim(), form.getPorcentajeBonus(), inicio, fin,
                    form.isActiva(), admin.getIdUsuario(), form.getIdCampana());
            if (filas != 1) throw new IllegalStateException("No encontramos esa campaña para editar.");
        }
    }

    @Transactional(readOnly = true)
    public List<ClubCampanaAdminDTO> listarCampanas() {
        return jdbc.query("""
                SELECT id_campana, nombre, porcentaje_bonus, fecha_inicio, fecha_fin, activa
                FROM club_campanas ORDER BY fecha_inicio DESC, id_campana DESC
                """, (rs, fila) -> new ClubCampanaAdminDTO(
                rs.getLong("id_campana"), rs.getString("nombre"), rs.getInt("porcentaje_bonus"),
                fecha(rs.getTimestamp("fecha_inicio")), fecha(rs.getTimestamp("fecha_fin")),
                rs.getBoolean("activa")
        ));
    }

    @Transactional(readOnly = true)
    public ClubCampanaFormDTO obtenerCampana(Long id) {
        return jdbc.query("""
                SELECT id_campana, nombre, porcentaje_bonus, fecha_inicio, fecha_fin, activa
                FROM club_campanas WHERE id_campana = ?
                """, rs -> {
            if (!rs.next()) throw new IllegalStateException("No encontramos esa campaña.");
            ClubCampanaFormDTO form = new ClubCampanaFormDTO();
            form.setIdCampana(rs.getLong("id_campana"));
            form.setNombre(rs.getString("nombre"));
            form.setPorcentajeBonus(rs.getInt("porcentaje_bonus"));
            form.setFechaInicio(fecha(rs.getTimestamp("fecha_inicio")).toLocalDate());
            form.setFechaFin(fecha(rs.getTimestamp("fecha_fin")).toLocalDate());
            form.setActiva(rs.getBoolean("activa"));
            return form;
        }, id);
    }

    @Transactional(readOnly = true)
    public List<ClubCanjeAdminDTO> listarCanjes(String busqueda) {
        String texto = busqueda == null ? "" : busqueda.trim();
        String criterio = "%" + texto + "%";
        return jdbc.query("""
                SELECT c.id_canje, CONCAT(u.nombres, ' ', u.apellidos) AS cliente,
                       u.dni, c.nombre_recompensa, c.codigo_cupon, c.categoria,
                       CASE WHEN c.estado IN ('DISPONIBLE', 'PENDIENTE_ENTREGA')
                                  AND c.vence_en <= NOW() THEN 'VENCIDO' ELSE c.estado END AS estado,
                       c.puntos_usados, c.fecha_canje, c.vence_en,
                       CONCAT(COALESCE(v.nombres, ''), ' ', COALESCE(v.apellidos, '')) AS validador
                FROM club_canjes c
                JOIN usuarios u ON u.id_usuario = c.id_usuario
                LEFT JOIN usuarios v ON v.id_usuario = c.id_usuario_validador
                WHERE (? = '' OR c.codigo_cupon LIKE ? OR u.dni LIKE ?
                       OR u.nombres LIKE ? OR u.apellidos LIKE ? OR c.nombre_recompensa LIKE ?)
                ORDER BY c.fecha_canje DESC, c.id_canje DESC LIMIT 200
                """, (rs, fila) -> new ClubCanjeAdminDTO(
                rs.getLong("id_canje"), rs.getString("cliente"), rs.getString("dni"),
                rs.getString("nombre_recompensa"), rs.getString("codigo_cupon"),
                rs.getString("categoria"), rs.getString("estado"), rs.getInt("puntos_usados"),
                fecha(rs.getTimestamp("fecha_canje")), fecha(rs.getTimestamp("vence_en")),
                StringUtils.trimWhitespace(rs.getString("validador"))
        ), texto, criterio, criterio, criterio, criterio, criterio);
    }

    @Transactional(readOnly = true)
    public List<ClubProductoDTO> listarProductosActivos() {
        return jdbc.query("""
                SELECT p.id_producto, p.nombre, p.imagen
                FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
                WHERE p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO'
                ORDER BY c.nombre, p.nombre
                """, (rs, fila) -> new ClubProductoDTO(
                rs.getInt("id_producto"), rs.getString("nombre"), rs.getString("imagen")
        ));
    }

    private void validarRecompensa(ClubRecompensaFormDTO form) {
        if (!TIPOS.contains(form.getTipo())) throw new IllegalStateException("Selecciona un tipo de recompensa válido.");
        if (!CATEGORIAS.contains(form.getCategoria())) throw new IllegalStateException("Selecciona una categoría válida.");
        if (form.getValor() == null || form.getValor() <= 0) throw new IllegalStateException("El valor de la recompensa debe ser mayor a cero.");
        if ("PRODUCTO_CARTA".equals(form.getTipo())) {
            if (form.getIdProducto() == null || jdbc.queryForObject("""
                    SELECT COUNT(*) FROM productos p JOIN categorias c ON c.id_categoria = p.id_categoria
                    WHERE p.id_producto = ? AND p.estado = 'ACTIVO' AND p.disponible = 1 AND c.estado = 'ACTIVO'
                    """, Integer.class, form.getIdProducto()) == 0) {
                throw new IllegalStateException("Vincula un producto activo y disponible de la carta.");
            }
        }
        if (form.getDisponibleDesde() != null && form.getDisponibleHasta() != null
                && form.getDisponibleHasta().isBefore(form.getDisponibleDesde())) {
            throw new IllegalStateException("La fecha final no puede preceder a la fecha de inicio.");
        }
        String imagen = normalizar(form.getImagen());
        if (imagen != null && !(imagen.startsWith("/img/") || imagen.startsWith("/productos/imagenes/"))) {
            throw new IllegalStateException("Usa una imagen existente del proyecto o sube una imagen.");
        }
    }

    private void guardarValor(String clave, Integer valor, Usuario admin) {
        jdbc.update("""
                INSERT INTO club_configuracion (clave, valor, id_admin) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE valor = VALUES(valor), id_admin = VALUES(id_admin)
                """, clave, valor.toString(), admin.getIdUsuario());
    }

    private int valorConfig(String clave, int predeterminado) {
        Integer valor = jdbc.query("SELECT valor FROM club_configuracion WHERE clave = ?",
                rs -> rs.next() ? Integer.valueOf(rs.getString(1)) : null, clave);
        return valor == null ? predeterminado : valor;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private LocalDateTime fecha(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
