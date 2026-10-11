package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteMetodoPagoDTO;
import com.rinconcito.dto.ReporteProductoDTO;
import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Usuario;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminReportePdfService {

    private static final float MARGEN = 30;

    private static final PDRectangle PAGINA_HORIZONTAL =
            new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth()
            );

    private static final float ANCHO_PAGINA =
            PAGINA_HORIZONTAL.getWidth();

    private static final float ALTO_PAGINA =
            PAGINA_HORIZONTAL.getHeight();

    private static final ZoneId ZONA_PERU =
            ZoneId.of("America/Lima");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    private final ConfiguracionRestauranteService
            configuracionRestauranteService;

    private final PDType1Font fuenteNormal =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    private final PDType1Font fuenteNegrita =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    public AdminReportePdfService(
            ConfiguracionRestauranteService
                    configuracionRestauranteService
    ) {
        this.configuracionRestauranteService =
                configuracionRestauranteService;
    }

    public byte[] generar(
            AdminReporteDTO reporte
    ) {
        if (reporte == null) {
            throw new IllegalStateException(
                    "No existen datos para generar el reporte."
            );
        }

        try (
                PDDocument documento =
                        new PDDocument();

                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream()
        ) {
            LienzoPdf lienzo =
                    new LienzoPdf(documento);

            lienzo.nuevaPagina();

            dibujarEncabezado(
                    lienzo,
                    reporte
            );

            dibujarResumen(
                    lienzo,
                    reporte
            );

            dibujarMetodosPago(
                    lienzo,
                    reporte
            );

            dibujarProductos(
                    lienzo,
                    reporte
            );

            lienzo.nuevaPagina();

            dibujarEncabezadoSecundario(
                    lienzo,
                    reporte,
                    "DETALLE DE OPERACIONES"
            );

            dibujarOperaciones(
                    lienzo,
                    reporte
            );

            lienzo.cerrar();

            documento.save(salida);

            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF.",
                    e
            );
        }
    }

    private void dibujarEncabezado(
            LienzoPdf lienzo,
            AdminReporteDTO reporte
    ) throws IOException {
        lienzo.textoCentrado(
                obtenerNombreRestaurante(),
                MARGEN,
                ANCHO_PAGINA - MARGEN * 2,
                lienzo.y,
                15,
                fuenteNegrita,
                140,
                47,
                27
        );

        lienzo.y -= 24;

        lienzo.textoCentrado(
                "REPORTE ADMINISTRATIVO",
                MARGEN,
                ANCHO_PAGINA - MARGEN * 2,
                lienzo.y,
                19,
                fuenteNegrita,
                140,
                47,
                27
        );

        lienzo.y -= 21;

        lienzo.textoCentrado(
                "Periodo: "
                        + reporte.getDesde()
                        .format(FORMATO_FECHA)
                        + " al "
                        + reporte.getHasta()
                        .format(FORMATO_FECHA),
                MARGEN,
                ANCHO_PAGINA - MARGEN * 2,
                lienzo.y,
                9,
                fuenteNormal,
                100,
                92,
                87
        );

        lienzo.y -= 15;

        lienzo.textoCentrado(
                "Generado: "
                        + LocalDateTime.now(ZONA_PERU)
                        .format(FORMATO_FECHA_HORA),
                MARGEN,
                ANCHO_PAGINA - MARGEN * 2,
                lienzo.y,
                8,
                fuenteNormal,
                120,
                112,
                107
        );

        lienzo.y -= 16;
        lienzo.linea(lienzo.y);
        lienzo.y -= 20;
    }

    private void dibujarEncabezadoSecundario(
            LienzoPdf lienzo,
            AdminReporteDTO reporte,
            String titulo
    ) throws IOException {
        lienzo.texto(
                obtenerNombreRestaurante(),
                MARGEN,
                lienzo.y,
                12,
                fuenteNegrita,
                140,
                47,
                27
        );

        lienzo.texto(
                titulo,
                310,
                lienzo.y,
                14,
                fuenteNegrita,
                140,
                47,
                27
        );

        lienzo.y -= 17;

        lienzo.texto(
                "Periodo: "
                        + reporte.getDesde()
                        .format(FORMATO_FECHA)
                        + " al "
                        + reporte.getHasta()
                        .format(FORMATO_FECHA),
                MARGEN,
                lienzo.y,
                8,
                fuenteNormal,
                100,
                92,
                87
        );

        lienzo.y -= 14;
        lienzo.linea(lienzo.y);
        lienzo.y -= 18;
    }

    private void dibujarResumen(
            LienzoPdf lienzo,
            AdminReporteDTO reporte
    ) throws IOException {
        lienzo.tituloSeccion(
                "RESUMEN GENERAL"
        );

        float[] anchos = {
                175, 90, 175, 90
        };

        lienzo.fila(
                new String[]{
                        "Ventas aprobadas",
                        formatoMoneda(
                                reporte.getTotalVentas()
                        ),
                        "Total de operaciones",
                        String.valueOf(
                                reporte.getTotalOperaciones()
                        )
                },
                anchos,
                24,
                8,
                true
        );

        lienzo.fila(
                new String[]{
                        "Pagos aprobados",
                        String.valueOf(
                                reporte.getAprobados()
                        ),
                        "Pagos rechazados",
                        String.valueOf(
                                reporte.getRechazados()
                        )
                },
                anchos,
                22,
                8,
                false
        );

        lienzo.fila(
                new String[]{
                        "Pendientes de validacion",
                        String.valueOf(
                                reporte.getPendientes()
                        ),
                        "Pedidos aprobados",
                        String.valueOf(
                                reporte.getPedidosAprobados()
                        )
                },
                anchos,
                22,
                8,
                false
        );

        lienzo.fila(
                new String[]{
                        "Reservas aprobadas",
                        String.valueOf(
                                reporte.getReservasAprobadas()
                        ),
                        "Periodo consultado",
                        reporte.getDesde()
                                .format(FORMATO_FECHA)
                                + " - "
                                + reporte.getHasta()
                                .format(FORMATO_FECHA)
                },
                anchos,
                22,
                8,
                false
        );

        lienzo.y -= 18;
    }

    private void dibujarMetodosPago(
            LienzoPdf lienzo,
            AdminReporteDTO reporte
    ) throws IOException {
        lienzo.tituloSeccion(
                "VENTAS POR METODO DE PAGO"
        );

        List<ReporteMetodoPagoDTO> metodos =
                reporte.getMetodosPago();

        if (metodos == null || metodos.isEmpty()) {
            lienzo.mensajeSinDatos();
            return;
        }

        float[] anchos = {
                240, 140, 170
        };

        lienzo.fila(
                new String[]{
                        "Metodo",
                        "Operaciones",
                        "Monto"
                },
                anchos,
                23,
                8,
                true
        );

        for (ReporteMetodoPagoDTO metodo : metodos) {
            lienzo.fila(
                    new String[]{
                            metodo.getMetodo(),
                            String.valueOf(
                                    metodo.getCantidad()
                            ),
                            formatoMoneda(
                                    metodo.getMonto()
                            )
                    },
                    anchos,
                    21,
                    8,
                    false
            );
        }

        lienzo.y -= 18;
    }

    private void dibujarProductos(
            LienzoPdf lienzo,
            AdminReporteDTO reporte
    ) throws IOException {
        lienzo.tituloSeccion(
                "PRODUCTOS MAS VENDIDOS"
        );

        List<ReporteProductoDTO> productos =
                reporte.getProductosVendidos();

        if (
                productos == null
                        || productos.isEmpty()
        ) {
            lienzo.mensajeSinDatos();
            return;
        }

        float[] anchos = {
                45, 390, 110, 150
        };

        dibujarCabeceraProductos(
                lienzo,
                anchos
        );

        int posicion = 1;

        for (
                ReporteProductoDTO producto
                : productos
        ) {
            if (lienzo.sinEspacio(22)) {
                lienzo.nuevaPagina();

                lienzo.texto(
                        "PRODUCTOS MAS VENDIDOS",
                        MARGEN,
                        lienzo.y,
                        13,
                        fuenteNegrita,
                        140,
                        47,
                        27
                );

                lienzo.y -= 22;

                dibujarCabeceraProductos(
                        lienzo,
                        anchos
                );
            }

            lienzo.fila(
                    new String[]{
                            String.valueOf(posicion++),
                            producto.getProducto(),
                            String.valueOf(
                                    producto.getCantidad()
                            ),
                            formatoMoneda(
                                    producto.getMonto()
                            )
                    },
                    anchos,
                    21,
                    8,
                    false
            );
        }
    }

    private void dibujarCabeceraProductos(
            LienzoPdf lienzo,
            float[] anchos
    ) throws IOException {
        lienzo.fila(
                new String[]{
                        "N.",
                        "Producto",
                        "Cantidad",
                        "Importe"
                },
                anchos,
                23,
                8,
                true
        );
    }

    private void dibujarOperaciones(
            LienzoPdf lienzo,
            AdminReporteDTO reporte
    ) throws IOException {
        List<Pago> operaciones =
                reporte.getOperaciones();

        if (
                operaciones == null
                        || operaciones.isEmpty()
        ) {
            lienzo.mensajeSinDatos();
            return;
        }

        float[] anchos = {
                55,
                90,
                145,
                60,
                70,
                85,
                105,
                62,
                100
        };

        dibujarCabeceraOperaciones(
                lienzo,
                anchos
        );

        for (Pago pago : operaciones) {
            if (lienzo.sinEspacio(22)) {
                lienzo.nuevaPagina();

                dibujarEncabezadoSecundario(
                        lienzo,
                        reporte,
                        "DETALLE DE OPERACIONES"
                );

                dibujarCabeceraOperaciones(
                        lienzo,
                        anchos
                );
            }

            Usuario cliente =
                    obtenerCliente(pago);

            lienzo.fila(
                    new String[]{
                            obtenerTipo(pago),
                            obtenerCodigo(pago),
                            nombreCompleto(cliente),
                            cliente != null
                                    ? textoSeguro(
                                            cliente.getDni()
                                    )
                                    : "-",
                            pago.getMetodoPago() != null
                                    ? pago.getMetodoPago()
                                    .name()
                                    : "-",
                            textoSeguro(
                                    pago.getNumeroOperacion()
                            ),
                            pago.getEstado() != null
                                    ? pago.getEstado()
                                    .name()
                                    : "-",
                            formatoMoneda(
                                    valorSeguro(
                                            pago.getMonto()
                                    )
                            ),
                            formatearFecha(
                                    obtenerFechaOperacion(
                                            pago
                                    )
                            )
                    },
                    anchos,
                    21,
                    6.4f,
                    false
            );
        }
    }

    private void dibujarCabeceraOperaciones(
            LienzoPdf lienzo,
            float[] anchos
    ) throws IOException {
        lienzo.fila(
                new String[]{
                        "Tipo",
                        "Codigo",
                        "Cliente",
                        "DNI",
                        "Metodo",
                        "Operacion",
                        "Estado",
                        "Monto",
                        "Fecha"
                },
                anchos,
                24,
                6.5f,
                true
        );
    }

    private String obtenerNombreRestaurante() {
        ConfiguracionRestaurante configuracion =
                configuracionRestauranteService
                        .obtener();

        if (
                configuracion != null
                        && configuracion.getNombre() != null
                        && !configuracion
                        .getNombre()
                        .isBlank()
        ) {
            return configuracion
                    .getNombre()
                    .trim();
        }

        return "El Rinconcito Ayacuchano Campestre";
    }

    private String obtenerTipo(
            Pago pago
    ) {
        if (pago.getPedido() != null) {
            return "PEDIDO";
        }

        if (pago.getReserva() != null) {
            return "RESERVA";
        }

        return "OTRO";
    }

    private String obtenerCodigo(
            Pago pago
    ) {
        if (pago.getPedido() != null) {
            return textoSeguro(
                    pago.getPedido()
                            .getCodigoPedido()
            );
        }

        if (pago.getReserva() != null) {
            return textoSeguro(
                    pago.getReserva()
                            .getCodigoReserva()
            );
        }

        return "-";
    }

    private Usuario obtenerCliente(
            Pago pago
    ) {
        if (pago.getPedido() != null) {
            return pago.getPedido().getCliente();
        }

        if (pago.getReserva() != null) {
            return pago.getReserva().getCliente();
        }

        return null;
    }

    private String nombreCompleto(
            Usuario usuario
    ) {
        if (usuario == null) {
            return "Sin cliente";
        }

        String nombres =
                usuario.getNombres() != null
                        ? usuario.getNombres().trim()
                        : "";

        String apellidos =
                usuario.getApellidos() != null
                        ? usuario.getApellidos().trim()
                        : "";

        String nombreCompleto =
                (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank()
                ? "Sin cliente"
                : nombreCompleto;
    }

    private LocalDateTime obtenerFechaOperacion(
            Pago pago
    ) {
        return pago.getFechaValidacion() != null
                ? pago.getFechaValidacion()
                : pago.getFechaPago();
    }

    private String formatearFecha(
            LocalDateTime fecha
    ) {
        return fecha != null
                ? fecha.format(
                        FORMATO_FECHA_HORA
                )
                : "-";
    }

    private String formatoMoneda(
            long monto
    ) {
        return "S/ " + monto;
    }

    private String textoSeguro(
            String texto
    ) {
        return texto != null
                && !texto.isBlank()
                ? texto.trim()
                : "-";
    }

    private long valorSeguro(
            Number numero
    ) {
        return numero != null
                ? numero.longValue()
                : 0L;
    }

    private String limpiarTexto(
            String texto
    ) {
        if (texto == null) {
            return "";
        }

        String limpio =
                texto
                        .replace("×", "x")
                        .replace("–", "-")
                        .replace("—", "-")
                        .replace("“", "\"")
                        .replace("”", "\"")
                        .replace("‘", "'")
                        .replace("’", "'")
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .replace("\t", " ");

        StringBuilder resultado =
                new StringBuilder();

        for (char caracter : limpio.toCharArray()) {
            if (
                    caracter >= 32
                            && caracter <= 255
            ) {
                resultado.append(caracter);

            } else if (
                    Character.isWhitespace(caracter)
            ) {
                resultado.append(' ');

            } else {
                resultado.append('?');
            }
        }

        return resultado.toString();
    }

    private String ajustarTexto(
            String texto,
            PDType1Font fuente,
            float tamano,
            float anchoMaximo
    ) throws IOException {
        String limpio =
                limpiarTexto(texto);

        if (limpio.isBlank()) {
            return "-";
        }

        if (
                anchoTexto(
                        limpio,
                        fuente,
                        tamano
                ) <= anchoMaximo
        ) {
            return limpio;
        }

        String sufijo = "...";
        String resultado = limpio;

        while (
                resultado.length() > 1
                        && anchoTexto(
                                resultado + sufijo,
                                fuente,
                                tamano
                        ) > anchoMaximo
        ) {
            resultado =
                    resultado.substring(
                            0,
                            resultado.length() - 1
                    );
        }

        return resultado.trim() + sufijo;
    }

    private float anchoTexto(
            String texto,
            PDType1Font fuente,
            float tamano
    ) throws IOException {
        return fuente.getStringWidth(texto)
                / 1000
                * tamano;
    }

    private class LienzoPdf {

        private final PDDocument documento;

        private PDPageContentStream contenido;

        private float y;

        private LienzoPdf(
                PDDocument documento
        ) {
            this.documento = documento;
        }

        private void nuevaPagina()
                throws IOException {
            if (contenido != null) {
                contenido.close();
            }

            PDPage pagina =
                    new PDPage(
                            PAGINA_HORIZONTAL
                    );

            documento.addPage(pagina);

            contenido =
                    new PDPageContentStream(
                            documento,
                            pagina
                    );

            y = ALTO_PAGINA - MARGEN;
        }

        private boolean sinEspacio(
                float altura
        ) {
            return y - altura < MARGEN;
        }

        private void tituloSeccion(
                String titulo
        ) throws IOException {
            if (sinEspacio(35)) {
                nuevaPagina();
            }

            texto(
                    titulo,
                    MARGEN,
                    y,
                    10,
                    fuenteNegrita,
                    140,
                    47,
                    27
            );

            y -= 18;
        }

        private void mensajeSinDatos()
                throws IOException {
            texto(
                    "No existen datos para este periodo.",
                    MARGEN,
                    y,
                    8,
                    fuenteNormal,
                    110,
                    102,
                    97
            );

            y -= 25;
        }

        private void fila(
                String[] valores,
                float[] anchos,
                float altura,
                float tamanoFuente,
                boolean encabezado
        ) throws IOException {
            float x = MARGEN;

            for (
                    int columna = 0;
                    columna < valores.length;
                    columna++
            ) {
                float ancho =
                        anchos[columna];

                if (encabezado) {
                    contenido.setNonStrokingColor(
                            140 / 255f,
                            47 / 255f,
                            27 / 255f
                    );

                    contenido.addRect(
                            x,
                            y - altura,
                            ancho,
                            altura
                    );

                    contenido.fill();

                } else {
                    contenido.setNonStrokingColor(
                            1f,
                            1f,
                            1f
                    );

                    contenido.addRect(
                            x,
                            y - altura,
                            ancho,
                            altura
                    );

                    contenido.fill();
                }

                contenido.setStrokingColor(
                        220 / 255f,
                        211 / 255f,
                        203 / 255f
                );

                contenido.setLineWidth(0.5f);

                contenido.addRect(
                        x,
                        y - altura,
                        ancho,
                        altura
                );

                contenido.stroke();

                PDType1Font fuente =
                        encabezado
                                ? fuenteNegrita
                                : fuenteNormal;

                String texto =
                        ajustarTexto(
                                valores[columna],
                                fuente,
                                tamanoFuente,
                                ancho - 8
                        );

                int color =
                        encabezado ? 255 : 59;

                this.texto(
                        texto,
                        x + 4,
                        y - altura
                                + (
                                altura - tamanoFuente
                        ) / 2
                                + 1,
                        tamanoFuente,
                        fuente,
                        color,
                        encabezado ? 255 : 42,
                        encabezado ? 255 : 34
                );

                x += ancho;
            }

            y -= altura;
        }

        private void texto(
                String texto,
                float x,
                float posicionY,
                float tamano,
                PDType1Font fuente,
                int rojo,
                int verde,
                int azul
        ) throws IOException {
            contenido.beginText();

            contenido.setFont(
                    fuente,
                    tamano
            );

            contenido.setNonStrokingColor(
                    rojo / 255f,
                    verde / 255f,
                    azul / 255f
            );

            contenido.newLineAtOffset(
                    x,
                    posicionY
            );

            contenido.showText(
                    limpiarTexto(texto)
            );

            contenido.endText();
        }

        private void textoCentrado(
                String texto,
                float inicioX,
                float ancho,
                float posicionY,
                float tamano,
                PDType1Font fuente,
                int rojo,
                int verde,
                int azul
        ) throws IOException {
            String textoLimpio =
                    limpiarTexto(texto);

            float anchoDelTexto =
                    anchoTexto(
                            textoLimpio,
                            fuente,
                            tamano
                    );

            float x =
                    inicioX
                            + (
                            ancho - anchoDelTexto
                    ) / 2;

            this.texto(
                    textoLimpio,
                    x,
                    posicionY,
                    tamano,
                    fuente,
                    rojo,
                    verde,
                    azul
            );
        }

        private void linea(
                float posicionY
        ) throws IOException {
            contenido.setStrokingColor(
                    140 / 255f,
                    47 / 255f,
                    27 / 255f
            );

            contenido.setLineWidth(1);

            contenido.moveTo(
                    MARGEN,
                    posicionY
            );

            contenido.lineTo(
                    ANCHO_PAGINA - MARGEN,
                    posicionY
            );

            contenido.stroke();
        }

        private void cerrar()
                throws IOException {
            if (contenido != null) {
                contenido.close();
                contenido = null;
            }
        }
    }
}