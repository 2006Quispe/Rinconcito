package com.rinconcito.service;

import com.rinconcito.entity.ConfiguracionRestaurante;
import com.rinconcito.entity.DetallePedido;
import com.rinconcito.entity.NotaVenta;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Pedido;
import com.rinconcito.entity.Reserva;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class NotaVentaPdfService {

    private static final float MARGEN = 45;
    private static final float ANCHO_PAGINA =
            PDRectangle.A4.getWidth();

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter FORMATO_DIA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    private final NotaVentaService notaVentaService;

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

    public NotaVentaPdfService(
            NotaVentaService notaVentaService,
            ConfiguracionRestauranteService configuracionRestauranteService
    ) {
        this.notaVentaService = notaVentaService;
        this.configuracionRestauranteService =
                configuracionRestauranteService;
    }

    public byte[] generar(Integer idNota) {

        NotaVenta notaVenta =
                notaVentaService.obtenerParaPdf(idNota);

        validarNota(notaVenta);

        Pago pago = notaVenta.getPago();

        boolean esPedido =
                pago.getPedido() != null;

        boolean esReserva =
                pago.getReserva() != null;

        List<DetallePedido> detalles =
                esPedido
                        ? notaVentaService.obtenerDetalles(notaVenta)
                        : Collections.emptyList();

        ConfiguracionRestaurante configuracion =
                configuracionRestauranteService.obtener();

        try (
                PDDocument documento = new PDDocument();
                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream()
        ) {
            LienzoPdf lienzo =
                    new LienzoPdf(documento);

            lienzo.nuevaPagina();

            dibujarEncabezado(
                    lienzo,
                    notaVenta,
                    configuracion
            );

            dibujarDatosCliente(
                    lienzo,
                    notaVenta
            );

            if (esPedido) {

                dibujarDatosEntrega(
                        lienzo,
                        pago.getPedido()
                );

                dibujarTablaProductos(
                        lienzo,
                        detalles
                );

                dibujarTotalesPedido(
                        lienzo,
                        notaVenta
                );
            }

            if (esReserva) {

                dibujarDetalleReserva(
                        lienzo,
                        pago.getReserva()
                );

                dibujarTotalReserva(
                        lienzo,
                        notaVenta
                );
            }

            dibujarInformacionPago(
                    lienzo,
                    notaVenta
            );

            dibujarPie(lienzo);

            lienzo.cerrar();

            documento.save(salida);

            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el PDF de la nota de venta.",
                    e
            );
        }
    }

    private void validarNota(NotaVenta notaVenta) {

        if (notaVenta == null) {
            throw new IllegalStateException(
                    "La nota de venta no existe."
            );
        }

        if (notaVenta.getPago() == null) {
            throw new IllegalStateException(
                    "La nota no tiene un pago asociado."
            );
        }

        Pago pago = notaVenta.getPago();

        if (pago.getPedido() == null
                && pago.getReserva() == null) {

            throw new IllegalStateException(
                    "El pago no pertenece a un pedido ni a una reserva."
            );
        }
    }

    private void dibujarEncabezado(
            LienzoPdf lienzo,
            NotaVenta nota,
            ConfiguracionRestaurante configuracion
    ) throws IOException {

        String nombreRestaurante =
                valor(
                        configuracion != null
                                ? configuracion.getNombre()
                                : null,
                        "El Rinconcito Ayacuchano Campestre"
                );

        String direccion =
                valor(
                        configuracion != null
                                ? configuracion.getDireccion()
                                : null,
                        "Ayacucho, Perú"
                );

        String telefono =
                valor(
                        configuracion != null
                                ? configuracion.getTelefono()
                                : null,
                        "-"
                );

        lienzo.texto(
                nombreRestaurante,
                MARGEN,
                lienzo.y,
                17,
                fuenteNegrita
        );

        lienzo.y -= 19;

        lienzo.texto(
                direccion,
                MARGEN,
                lienzo.y,
                9,
                fuenteNormal
        );

        lienzo.y -= 14;

        lienzo.texto(
                "Teléfono: " + telefono,
                MARGEN,
                lienzo.y,
                9,
                fuenteNormal
        );

        float cajaX = 360;
        float cajaY = 748;
        float cajaAncho = 190;
        float cajaAlto = 67;

        lienzo.rectangulo(
                cajaX,
                cajaY,
                cajaAncho,
                cajaAlto,
                140,
                47,
                27
        );

        lienzo.textoCentrado(
                "NOTA DE VENTA",
                cajaX,
                cajaAncho,
                cajaY + 43,
                14,
                fuenteNegrita
        );

        lienzo.textoCentrado(
                notaVentaService.formarNumero(nota),
                cajaX,
                cajaAncho,
                cajaY + 20,
                12,
                fuenteNegrita
        );

        lienzo.y -= 24;
        lienzo.linea(lienzo.y);
        lienzo.y -= 21;

        String fecha =
                nota.getFechaEmision() != null
                        ? nota.getFechaEmision().format(FORMATO_FECHA)
                        : "-";

        lienzo.texto(
                "Fecha de emisión: " + fecha,
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.texto(
                "Estado: " + nota.getEstado(),
                390,
                lienzo.y,
                10,
                fuenteNegrita
        );

        lienzo.y -= 22;
    }

    private void dibujarDatosCliente(
            LienzoPdf lienzo,
            NotaVenta nota
    ) throws IOException {

        Pago pago = nota.getPago();

        Usuario cliente;
        String codigo;
        String tipo;

        if (pago.getPedido() != null) {

            Pedido pedido = pago.getPedido();

            cliente = pedido.getCliente();

            codigo =
                    valor(
                            pedido.getCodigoPedido(),
                            "-"
                    );

            tipo = nombreModalidad(pedido);

        } else {

            Reserva reserva = pago.getReserva();

            cliente = reserva.getCliente();

            codigo =
                    valor(
                            reserva.getCodigoReserva(),
                            "-"
                    );

            tipo = "RESERVA";
        }

        lienzo.tituloSeccion(
                "DATOS DEL CLIENTE"
        );

        lienzo.texto(
                "Cliente: "
                        + valor(
                        nota.getClienteNombre(),
                        "Cliente"
                ),
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 16;

        lienzo.texto(
                "DNI: "
                        + valor(
                        nota.getClienteDocumento(),
                        "No registrado"
                ),
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        String telefonoCliente =
                cliente != null
                        ? valor(cliente.getTelefono(), "-")
                        : "-";

        lienzo.texto(
                "Teléfono: " + telefonoCliente,
                300,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 16;

        String etiquetaCodigo =
                pago.getPedido() != null
                        ? "Código de pedido: "
                        : "Código de reserva: ";

        lienzo.texto(
                etiquetaCodigo + codigo,
                MARGEN,
                lienzo.y,
                10,
                fuenteNegrita
        );

        lienzo.texto(
                "Tipo: " + tipo,
                350,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 22;
    }

    private void dibujarDatosEntrega(
            LienzoPdf lienzo,
            Pedido pedido
    ) throws IOException {

        lienzo.asegurarEspacio(105);

        lienzo.tituloSeccion(
                "MODALIDAD DE ENTREGA"
        );

        String modalidad =
                nombreModalidad(pedido);

        lienzo.texto(
                "Modalidad: " + modalidad,
                MARGEN,
                lienzo.y,
                10,
                fuenteNegrita
        );

        lienzo.y -= 18;

        if (esDelivery(pedido)) {

            if (pedido.getDireccion() != null) {

                String direccion =
                        valor(
                                pedido.getDireccion().getDireccion(),
                                "-"
                        );

                String distrito =
                        valor(
                                pedido.getDireccion().getDistrito(),
                                "-"
                        );

                lienzo.texto(
                        "Dirección: "
                                + recortar(
                                direccion
                                        + " - "
                                        + distrito,
                                76
                        ),
                        MARGEN,
                        lienzo.y,
                        10,
                        fuenteNormal
                );

                lienzo.y -= 17;

                String referencia =
                        valor(
                                pedido.getDireccion().getReferencia(),
                                "Sin referencia"
                        );

                lienzo.texto(
                        "Referencia: "
                                + recortar(referencia, 75),
                        MARGEN,
                        lienzo.y,
                        10,
                        fuenteNormal
                );

            } else {
                lienzo.texto(
                        "Dirección de entrega no registrada.",
                        MARGEN,
                        lienzo.y,
                        10,
                        fuenteNormal
                );
            }

        } else if (esRecojoLocal(pedido)) {

            lienzo.texto(
                    "El cliente recogerá el pedido en el restaurante.",
                    MARGEN,
                    lienzo.y,
                    10,
                    fuenteNormal
            );

        } else {

            lienzo.texto(
                    "Pedido para atención presencial en el restaurante.",
                    MARGEN,
                    lienzo.y,
                    10,
                    fuenteNormal
            );
        }

        lienzo.y -= 25;
    }

    private void dibujarTablaProductos(
            LienzoPdf lienzo,
            List<DetallePedido> detalles
    ) throws IOException {

        dibujarCabeceraTabla(lienzo);
        lienzo.paginaNueva = false;

        if (detalles == null || detalles.isEmpty()) {

            lienzo.texto(
                    "No se encontraron productos.",
                    MARGEN + 8,
                    lienzo.y,
                    9,
                    fuenteNormal
            );

            lienzo.y -= 30;
            return;
        }

        for (DetallePedido detalle : detalles) {

            lienzo.asegurarEspacio(36);

            if (lienzo.paginaNueva) {
                dibujarCabeceraTabla(lienzo);
                lienzo.paginaNueva = false;
            }

            String producto =
                    detalle.getProducto() != null
                            ? detalle.getProducto().getNombre()
                            : "Producto";

            producto =
                    recortar(producto, 38);

            int cantidad =
                    detalle.getCantidad() != null
                            ? detalle.getCantidad()
                            : 0;

            int precioUnitario =
                    detalle.getPrecioUnitario() != null
                            ? detalle.getPrecioUnitario()
                            : 0;

            int subtotal =
                    detalle.getSubtotal() != null
                            ? detalle.getSubtotal()
                            : 0;

            lienzo.texto(
                    producto,
                    MARGEN + 8,
                    lienzo.y,
                    9,
                    fuenteNormal
            );

            lienzo.texto(
                    String.valueOf(cantidad),
                    348,
                    lienzo.y,
                    9,
                    fuenteNormal
            );

            lienzo.texto(
                    "S/ " + precioUnitario,
                    412,
                    lienzo.y,
                    9,
                    fuenteNormal
            );

            lienzo.texto(
                    "S/ " + subtotal,
                    505,
                    lienzo.y,
                    9,
                    fuenteNegrita
            );

            lienzo.lineaSuave(
                    lienzo.y - 11
            );

            lienzo.y -= 27;
        }

        lienzo.y -= 5;
    }

    private void dibujarCabeceraTabla(
            LienzoPdf lienzo
    ) throws IOException {

        lienzo.tituloSeccion(
                "DETALLE DEL PEDIDO"
        );

        lienzo.relleno(
                MARGEN,
                lienzo.y - 5,
                ANCHO_PAGINA - (MARGEN * 2),
                22,
                140,
                47,
                27
        );

        lienzo.texto(
                "Producto",
                MARGEN + 5,
                lienzo.y + 2,
                9,
                fuenteNegrita,
                255,
                255,
                255
        );

        lienzo.texto(
                "Cant.",
                340,
                lienzo.y + 2,
                9,
                fuenteNegrita,
                255,
                255,
                255
        );

        lienzo.texto(
                "P. unitario",
                400,
                lienzo.y + 2,
                9,
                fuenteNegrita,
                255,
                255,
                255
        );

        lienzo.texto(
                "Subtotal",
                495,
                lienzo.y + 2,
                9,
                fuenteNegrita,
                255,
                255,
                255
        );

        lienzo.y -= 27;
    }

    private void dibujarDetalleReserva(
            LienzoPdf lienzo,
            Reserva reserva
    ) throws IOException {

        lienzo.asegurarEspacio(180);

        lienzo.tituloSeccion(
                "DETALLE DE LA RESERVA"
        );

        lienzo.relleno(
                MARGEN,
                lienzo.y - 5,
                ANCHO_PAGINA - (MARGEN * 2),
                22,
                140,
                47,
                27
        );

        lienzo.texto(
                "Información de la reserva",
                MARGEN + 8,
                lienzo.y + 2,
                9,
                fuenteNegrita,
                255,
                255,
                255
        );

        lienzo.y -= 32;

        String evento =
                reserva.getEvento() != null
                        ? valor(
                        reserva.getEvento().getTitulo(),
                        "Sin evento"
                )
                        : "Sin evento";

        lienzo.texto(
                "Evento: " + recortar(evento, 65),
                MARGEN + 8,
                lienzo.y,
                10,
                fuenteNegrita
        );

        lienzo.y -= 22;

        String fecha =
                reserva.getFechaReserva() != null
                        ? reserva.getFechaReserva().format(FORMATO_DIA)
                        : "-";

        String horaInicio =
                reserva.getHoraInicio() != null
                        ? reserva.getHoraInicio().format(FORMATO_HORA)
                        : "-";

        String horaFin =
                reserva.getHoraFin() != null
                        ? reserva.getHoraFin().format(FORMATO_HORA)
                        : "-";

        lienzo.texto(
                "Fecha: " + fecha,
                MARGEN + 8,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.texto(
                "Horario: "
                        + horaInicio
                        + " - "
                        + horaFin,
                300,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 22;

        String piso =
                reserva.getPisoSolicitado() != null
                        ? valor(
                        reserva.getPisoSolicitado().getNombre(),
                        "-"
                )
                        : "-";

        String mesa =
                reserva.getMesa() != null
                        && reserva.getMesa().getNumero() != null
                        ? String.valueOf(
                        reserva.getMesa().getNumero()
                )
                        : "Por asignar";

        lienzo.texto(
                "Piso: " + piso,
                MARGEN + 8,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.texto(
                "Mesa: " + mesa,
                300,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 22;

        int personas =
                reserva.getNumeroPersonas() != null
                        ? reserva.getNumeroPersonas()
                        : 0;

        lienzo.texto(
                "Número de personas: " + personas,
                MARGEN + 8,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.lineaSuave(
                lienzo.y - 12
        );

        lienzo.y -= 30;
    }

    private void dibujarTotalesPedido(
            LienzoPdf lienzo,
            NotaVenta nota
    ) throws IOException {

        lienzo.asegurarEspacio(130);

        Pedido pedido =
                nota.getPago().getPedido();

        int subtotal =
                pedido.getSubtotal() != null
                        ? pedido.getSubtotal()
                        : 0;

        int costoDelivery =
                pedido.getCostoDelivery() != null
                        ? pedido.getCostoDelivery()
                        : 0;

        int total =
                nota.getTotal() != null
                        ? nota.getTotal()
                        : 0;

        float inicioX = 355;

        lienzo.texto(
                "Subtotal:",
                inicioX,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.texto(
                "S/ " + subtotal,
                500,
                lienzo.y,
                10,
                fuenteNegrita
        );

        lienzo.y -= 21;

        if (esDelivery(pedido)) {

            lienzo.texto(
                    "Delivery:",
                    inicioX,
                    lienzo.y,
                    10,
                    fuenteNormal
            );

            if (costoDelivery > 0) {

                lienzo.texto(
                        "S/ " + costoDelivery,
                        500,
                        lienzo.y,
                        10,
                        fuenteNegrita
                );

            } else {

                lienzo.texto(
                        "Por coordinar",
                        465,
                        lienzo.y,
                        10,
                        fuenteNormal
                );
            }

        } else if (esRecojoLocal(pedido)) {

            lienzo.texto(
                    "Recojo en local:",
                    inicioX,
                    lienzo.y,
                    10,
                    fuenteNormal
            );

            lienzo.texto(
                    "Sin costo",
                    492,
                    lienzo.y,
                    10,
                    fuenteNegrita
            );

        } else {

            lienzo.texto(
                    "Atención:",
                    inicioX,
                    lienzo.y,
                    10,
                    fuenteNormal
            );

            lienzo.texto(
                    "Presencial",
                    483,
                    lienzo.y,
                    10,
                    fuenteNegrita
            );
        }

        lienzo.y -= 38;

        dibujarCajaTotal(
                lienzo,
                total
        );
    }

    private void dibujarTotalReserva(
            LienzoPdf lienzo,
            NotaVenta nota
    ) throws IOException {

        lienzo.asegurarEspacio(90);

        int total =
                nota.getTotal() != null
                        ? nota.getTotal()
                        : 0;

        dibujarCajaTotal(
                lienzo,
                total
        );
    }

    private void dibujarCajaTotal(
            LienzoPdf lienzo,
            int total
    ) throws IOException {

        float inicioX = 355;

        lienzo.relleno(
                inicioX - 10,
                lienzo.y - 7,
                205,
                34,
                247,
                237,
                207
        );

        lienzo.texto(
                "TOTAL:",
                inicioX,
                lienzo.y + 4,
                13,
                fuenteNegrita
        );

        lienzo.texto(
                "S/ " + total,
                490,
                lienzo.y + 4,
                14,
                fuenteNegrita,
                140,
                47,
                27
        );

        lienzo.y -= 48;
    }

    private void dibujarInformacionPago(
            LienzoPdf lienzo,
            NotaVenta nota
    ) throws IOException {

        lienzo.asegurarEspacio(100);

        Pago pago = nota.getPago();

        lienzo.tituloSeccion(
                "INFORMACIÓN DEL PAGO"
        );

        lienzo.texto(
                "Medio de pago: "
                        + (
                        pago.getMetodoPago() != null
                                ? pago.getMetodoPago()
                                : "-"
                ),
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 16;

        lienzo.texto(
                "Número de operación: "
                        + valor(
                        pago.getNumeroOperacion(),
                        "No registrado"
                ),
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 16;

        String caja =
                nota.getUsuarioEmisor() != null
                        ? construirNombre(
                        nota.getUsuarioEmisor()
                )
                        : "-";

        lienzo.texto(
                "Emitido por: " + caja,
                MARGEN,
                lienzo.y,
                10,
                fuenteNormal
        );

        lienzo.y -= 25;
    }

    private void dibujarPie(
            LienzoPdf lienzo
    ) throws IOException {

        lienzo.asegurarEspacio(75);

        lienzo.linea(lienzo.y);
        lienzo.y -= 18;

        lienzo.textoCentrado(
                "Gracias por su preferencia",
                MARGEN,
                ANCHO_PAGINA - (MARGEN * 2),
                lienzo.y,
                11,
                fuenteNegrita
        );

        lienzo.y -= 17;

        lienzo.textoCentrado(
                "Documento interno. "
                        + "No válido como comprobante electrónico SUNAT.",
                MARGEN,
                ANCHO_PAGINA - (MARGEN * 2),
                lienzo.y,
                8,
                fuenteNormal
        );
    }

    private boolean esDelivery(Pedido pedido) {

        return pedido != null
                && pedido.getTipoPedido()
                == Pedido.TipoPedido.DELIVERY;
    }

    private boolean esRecojoLocal(Pedido pedido) {

        return pedido != null
                && pedido.getTipoPedido()
                == Pedido.TipoPedido.RECOJO_LOCAL;
    }

    private String nombreModalidad(Pedido pedido) {

        if (pedido == null
                || pedido.getTipoPedido() == null) {
            return "PEDIDO";
        }

        if (esDelivery(pedido)) {
            return "DELIVERY";
        }

        if (esRecojoLocal(pedido)) {
            return "RECOJO EN LOCAL";
        }

        return "PRESENCIAL";
    }

    private String construirNombre(
            Usuario usuario
    ) {

        String nombres =
                valor(
                        usuario.getNombres(),
                        ""
                );

        String apellidos =
                valor(
                        usuario.getApellidos(),
                        ""
                );

        String nombreCompleto =
                (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank()
                ? "-"
                : nombreCompleto;
    }

    private String valor(
            String texto,
            String valorDefecto
    ) {

        if (texto == null || texto.isBlank()) {
            return valorDefecto;
        }

        return texto.trim();
    }

    private String recortar(
            String texto,
            int longitud
    ) {

        if (texto == null) {
            return "";
        }

        if (texto.length() <= longitud) {
            return texto;
        }

        return texto.substring(
                0,
                longitud - 3
        ) + "...";
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

            if (caracter >= 32
                    && caracter <= 255) {

                resultado.append(caracter);

            } else if (Character.isWhitespace(caracter)) {

                resultado.append(' ');

            } else {

                resultado.append('?');
            }
        }

        return resultado.toString();
    }

    private class LienzoPdf {

        private final PDDocument documento;

        private PDPageContentStream contenido;

        private float y;

        private boolean paginaNueva;

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
                            PDRectangle.A4
                    );

            documento.addPage(pagina);

            contenido =
                    new PDPageContentStream(
                            documento,
                            pagina
                    );

            y = 790;
            paginaNueva = true;
        }

        private void asegurarEspacio(
                float espacio
        ) throws IOException {

            if (y - espacio < 70) {
                nuevaPagina();
            }
        }

        private void texto(
                String texto,
                float x,
                float posicionY,
                float tamano,
                PDType1Font fuente
        ) throws IOException {

            texto(
                    texto,
                    x,
                    posicionY,
                    tamano,
                    fuente,
                    59,
                    42,
                    34
            );
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
                PDType1Font fuente
        ) throws IOException {

            String textoLimpio =
                    limpiarTexto(texto);

            float anchoTexto =
                    fuente.getStringWidth(
                            textoLimpio
                    )
                            / 1000
                            * tamano;

            float x =
                    inicioX
                            + (
                            ancho - anchoTexto
                    ) / 2;

            texto(
                    textoLimpio,
                    x,
                    posicionY,
                    tamano,
                    fuente
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

        private void lineaSuave(
                float posicionY
        ) throws IOException {

            contenido.setStrokingColor(
                    225 / 255f,
                    217 / 255f,
                    208 / 255f
            );

            contenido.setLineWidth(0.5f);

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

        private void rectangulo(
                float x,
                float posicionY,
                float ancho,
                float alto,
                int rojo,
                int verde,
                int azul
        ) throws IOException {

            contenido.setStrokingColor(
                    rojo / 255f,
                    verde / 255f,
                    azul / 255f
            );

            contenido.setLineWidth(1.2f);

            contenido.addRect(
                    x,
                    posicionY,
                    ancho,
                    alto
            );

            contenido.stroke();
        }

        private void relleno(
                float x,
                float posicionY,
                float ancho,
                float alto,
                int rojo,
                int verde,
                int azul
        ) throws IOException {

            contenido.setNonStrokingColor(
                    rojo / 255f,
                    verde / 255f,
                    azul / 255f
            );

            contenido.addRect(
                    x,
                    posicionY,
                    ancho,
                    alto
            );

            contenido.fill();
        }

        private void tituloSeccion(
                String titulo
        ) throws IOException {

            asegurarEspacio(45);

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

        private void cerrar()
                throws IOException {

            if (contenido != null) {
                contenido.close();
                contenido = null;
            }
        }
    }
}