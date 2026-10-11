package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ReporteDiaDTO;
import com.rinconcito.dto.ReporteMetodoPagoDTO;
import com.rinconcito.dto.ReporteProductoDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.entity.Usuario;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminReporteExcelService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );

    public byte[] generar(
            AdminReporteDTO reporte
    ) {
        try (
                Workbook workbook =
                        new XSSFWorkbook();

                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream()
        ) {
            EstilosExcel estilos =
                    crearEstilos(workbook);

            crearHojaResumen(
                    workbook,
                    reporte,
                    estilos
            );

            crearHojaOperaciones(
                    workbook,
                    reporte,
                    estilos
            );

            crearHojaVentasDiarias(
                    workbook,
                    reporte,
                    estilos
            );

            crearHojaMetodosPago(
                    workbook,
                    reporte,
                    estilos
            );

            crearHojaProductos(
                    workbook,
                    reporte,
                    estilos
            );

            workbook.write(salida);

            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte Excel.",
                    e
            );
        }
    }

    private void crearHojaResumen(
            Workbook workbook,
            AdminReporteDTO reporte,
            EstilosExcel estilos
    ) {
        Sheet hoja =
                workbook.createSheet("Resumen");

        crearTitulo(
                hoja,
                "Reporte administrativo",
                1,
                estilos
        );

        int filaActual = 2;

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Periodo",
                reporte.getDesde().format(FORMATO_FECHA)
                        + " al "
                        + reporte.getHasta().format(
                                FORMATO_FECHA
                        ),
                estilos
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Ventas aprobadas",
                reporte.getTotalVentas(),
                estilos,
                true
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Total de operaciones",
                reporte.getTotalOperaciones(),
                estilos,
                false
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Pagos aprobados",
                reporte.getAprobados(),
                estilos,
                false
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Pagos rechazados",
                reporte.getRechazados(),
                estilos,
                false
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Pendientes de validacion",
                reporte.getPendientes(),
                estilos,
                false
        );

        filaActual = agregarDatoResumen(
                hoja,
                filaActual,
                "Pedidos aprobados",
                reporte.getPedidosAprobados(),
                estilos,
                false
        );

        agregarDatoResumen(
                hoja,
                filaActual,
                "Reservas aprobadas",
                reporte.getReservasAprobadas(),
                estilos,
                false
        );

        hoja.setColumnWidth(0, 30 * 256);
        hoja.setColumnWidth(1, 28 * 256);
    }

    private void crearHojaOperaciones(
            Workbook workbook,
            AdminReporteDTO reporte,
            EstilosExcel estilos
    ) {
        Sheet hoja =
                workbook.createSheet("Operaciones");

        String[] encabezados = {
                "Tipo",
                "Codigo",
                "Cliente",
                "Documento",
                "Metodo",
                "N. operacion",
                "Estado",
                "Monto",
                "Fecha"
        };

        crearTitulo(
                hoja,
                "Detalle de operaciones",
                encabezados.length - 1,
                estilos
        );

        crearEncabezados(
                hoja,
                2,
                encabezados,
                estilos
        );

        int numeroFila = 3;

        for (Pago pago : reporte.getOperaciones()) {
            Row fila =
                    hoja.createRow(numeroFila++);

            agregarTexto(
                    fila,
                    0,
                    obtenerTipo(pago),
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    1,
                    obtenerCodigo(pago),
                    estilos.dato
            );

            Usuario cliente =
                    obtenerCliente(pago);

            agregarTexto(
                    fila,
                    2,
                    nombreCompleto(cliente),
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    3,
                    cliente != null
                            ? textoSeguro(cliente.getDni())
                            : "-",
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    4,
                    pago.getMetodoPago() != null
                            ? pago.getMetodoPago().name()
                            : "-",
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    5,
                    textoSeguro(
                            pago.getNumeroOperacion()
                    ),
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    6,
                    pago.getEstado() != null
                            ? pago.getEstado().name()
                            : "-",
                    estilos.dato
            );

            agregarNumero(
                    fila,
                    7,
                    valorSeguro(pago.getMonto()),
                    estilos.moneda
            );

            agregarTexto(
                    fila,
                    8,
                    formatearFecha(
                            obtenerFechaOperacion(pago)
                    ),
                    estilos.dato
            );
        }

        int[] anchos = {
                15, 22, 30, 15, 18,
                20, 24, 15, 20
        };

        aplicarAnchos(hoja, anchos);

        hoja.createFreezePane(0, 3);
        hoja.setAutoFilter(
                new org.apache.poi.ss.util.CellRangeAddress(
                        2,
                        2,
                        0,
                        encabezados.length - 1
                )
        );
    }

    private void crearHojaVentasDiarias(
            Workbook workbook,
            AdminReporteDTO reporte,
            EstilosExcel estilos
    ) {
        Sheet hoja =
                workbook.createSheet("Ventas diarias");

        crearTitulo(
                hoja,
                "Ventas aprobadas por dia",
                2,
                estilos
        );

        crearEncabezados(
                hoja,
                2,
                new String[]{
                        "Fecha",
                        "Etiqueta",
                        "Monto"
                },
                estilos
        );

        int numeroFila = 3;

        for (
                ReporteDiaDTO dia
                : reporte.getVentasDiarias()
        ) {
            Row fila =
                    hoja.createRow(numeroFila++);

            agregarTexto(
                    fila,
                    0,
                    dia.getFecha().format(
                            FORMATO_FECHA
                    ),
                    estilos.dato
            );

            agregarTexto(
                    fila,
                    1,
                    dia.getEtiqueta(),
                    estilos.dato
            );

            agregarNumero(
                    fila,
                    2,
                    dia.getMonto(),
                    estilos.moneda
            );
        }

        aplicarAnchos(
                hoja,
                new int[]{18, 18, 18}
        );

        hoja.createFreezePane(0, 3);
    }

    private void crearHojaMetodosPago(
            Workbook workbook,
            AdminReporteDTO reporte,
            EstilosExcel estilos
    ) {
        Sheet hoja =
                workbook.createSheet("Metodos de pago");

        crearTitulo(
                hoja,
                "Resumen por metodo de pago",
                2,
                estilos
        );

        crearEncabezados(
                hoja,
                2,
                new String[]{
                        "Metodo",
                        "Operaciones",
                        "Monto"
                },
                estilos
        );

        int numeroFila = 3;

        for (
                ReporteMetodoPagoDTO metodo
                : reporte.getMetodosPago()
        ) {
            Row fila =
                    hoja.createRow(numeroFila++);

            agregarTexto(
                    fila,
                    0,
                    metodo.getMetodo(),
                    estilos.dato
            );

            agregarNumero(
                    fila,
                    1,
                    metodo.getCantidad(),
                    estilos.numero
            );

            agregarNumero(
                    fila,
                    2,
                    metodo.getMonto(),
                    estilos.moneda
            );
        }

        aplicarAnchos(
                hoja,
                new int[]{24, 18, 18}
        );

        hoja.createFreezePane(0, 3);
    }

    private void crearHojaProductos(
            Workbook workbook,
            AdminReporteDTO reporte,
            EstilosExcel estilos
    ) {
        Sheet hoja =
                workbook.createSheet("Productos vendidos");

        crearTitulo(
                hoja,
                "Productos mas vendidos",
                2,
                estilos
        );

        crearEncabezados(
                hoja,
                2,
                new String[]{
                        "Producto",
                        "Cantidad",
                        "Importe"
                },
                estilos
        );

        int numeroFila = 3;

        for (
                ReporteProductoDTO producto
                : reporte.getProductosVendidos()
        ) {
            Row fila =
                    hoja.createRow(numeroFila++);

            agregarTexto(
                    fila,
                    0,
                    producto.getProducto(),
                    estilos.dato
            );

            agregarNumero(
                    fila,
                    1,
                    producto.getCantidad(),
                    estilos.numero
            );

            agregarNumero(
                    fila,
                    2,
                    producto.getMonto(),
                    estilos.moneda
            );
        }

        aplicarAnchos(
                hoja,
                new int[]{40, 18, 18}
        );

        hoja.createFreezePane(0, 3);
    }

    private void crearTitulo(
            Sheet hoja,
            String titulo,
            int ultimaColumna,
            EstilosExcel estilos
    ) {
        Row fila =
                hoja.createRow(0);

        fila.setHeightInPoints(30);

        Cell celda =
                fila.createCell(0);

        celda.setCellValue(titulo);
        celda.setCellStyle(estilos.titulo);

        hoja.addMergedRegion(
                new org.apache.poi.ss.util.CellRangeAddress(
                        0,
                        0,
                        0,
                        ultimaColumna
                )
        );
    }

    private void crearEncabezados(
            Sheet hoja,
            int numeroFila,
            String[] encabezados,
            EstilosExcel estilos
    ) {
        Row fila =
                hoja.createRow(numeroFila);

        fila.setHeightInPoints(23);

        for (
                int columna = 0;
                columna < encabezados.length;
                columna++
        ) {
            Cell celda =
                    fila.createCell(columna);

            celda.setCellValue(
                    encabezados[columna]
            );

            celda.setCellStyle(
                    estilos.encabezado
            );
        }
    }

    private int agregarDatoResumen(
            Sheet hoja,
            int numeroFila,
            String etiqueta,
            String valor,
            EstilosExcel estilos
    ) {
        Row fila =
                hoja.createRow(numeroFila);

        agregarTexto(
                fila,
                0,
                etiqueta,
                estilos.etiquetaResumen
        );

        agregarTexto(
                fila,
                1,
                valor,
                estilos.dato
        );

        return numeroFila + 1;
    }

    private int agregarDatoResumen(
            Sheet hoja,
            int numeroFila,
            String etiqueta,
            long valor,
            EstilosExcel estilos,
            boolean moneda
    ) {
        Row fila =
                hoja.createRow(numeroFila);

        agregarTexto(
                fila,
                0,
                etiqueta,
                estilos.etiquetaResumen
        );

        agregarNumero(
                fila,
                1,
                valor,
                moneda
                        ? estilos.moneda
                        : estilos.numero
        );

        return numeroFila + 1;
    }

    private void agregarTexto(
            Row fila,
            int columna,
            String valor,
            CellStyle estilo
    ) {
        Cell celda =
                fila.createCell(columna);

        celda.setCellValue(
                valor != null ? valor : "-"
        );

        celda.setCellStyle(estilo);
    }

    private void agregarNumero(
            Row fila,
            int columna,
            long valor,
            CellStyle estilo
    ) {
        Cell celda =
                fila.createCell(columna);

        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void aplicarAnchos(
            Sheet hoja,
            int[] anchos
    ) {
        for (
                int columna = 0;
                columna < anchos.length;
                columna++
        ) {
            hoja.setColumnWidth(
                    columna,
                    anchos[columna] * 256
            );
        }
    }

    private EstilosExcel crearEstilos(
            Workbook workbook
    ) {
        CellStyle titulo =
                workbook.createCellStyle();

        titulo.setFillForegroundColor(
                IndexedColors.DARK_RED.getIndex()
        );

        titulo.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        titulo.setAlignment(
                HorizontalAlignment.CENTER
        );

        titulo.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        Font fuenteTitulo =
                workbook.createFont();

        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints(
                (short) 15
        );

        fuenteTitulo.setColor(
                IndexedColors.WHITE.getIndex()
        );

        titulo.setFont(fuenteTitulo);

        CellStyle encabezado =
                workbook.createCellStyle();

        encabezado.setFillForegroundColor(
                IndexedColors.GOLD.getIndex()
        );

        encabezado.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        encabezado.setAlignment(
                HorizontalAlignment.CENTER
        );

        encabezado.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        encabezado.setBorderBottom(
                BorderStyle.THIN
        );

        encabezado.setBorderTop(
                BorderStyle.THIN
        );

        encabezado.setBorderLeft(
                BorderStyle.THIN
        );

        encabezado.setBorderRight(
                BorderStyle.THIN
        );

        Font fuenteEncabezado =
                workbook.createFont();

        fuenteEncabezado.setBold(true);
        encabezado.setFont(fuenteEncabezado);

        CellStyle dato =
                workbook.createCellStyle();

        dato.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        dato.setBorderBottom(
                BorderStyle.THIN
        );

        dato.setBorderTop(
                BorderStyle.THIN
        );

        dato.setBorderLeft(
                BorderStyle.THIN
        );

        dato.setBorderRight(
                BorderStyle.THIN
        );

        CellStyle numero =
                workbook.createCellStyle();

        numero.cloneStyleFrom(dato);

        numero.setAlignment(
                HorizontalAlignment.RIGHT
        );

        numero.setDataFormat(
                workbook.createDataFormat()
                        .getFormat("#,##0")
        );

        CellStyle moneda =
                workbook.createCellStyle();

        moneda.cloneStyleFrom(dato);

        moneda.setAlignment(
                HorizontalAlignment.RIGHT
        );

        moneda.setDataFormat(
                workbook.createDataFormat()
                        .getFormat(
                                "\"S/ \"#,##0"
                        )
        );

        CellStyle etiquetaResumen =
                workbook.createCellStyle();

        etiquetaResumen.cloneStyleFrom(dato);

        etiquetaResumen.setFillForegroundColor(
                IndexedColors.LIGHT_ORANGE
                        .getIndex()
        );

        etiquetaResumen.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        Font fuenteEtiqueta =
                workbook.createFont();

        fuenteEtiqueta.setBold(true);

        etiquetaResumen.setFont(
                fuenteEtiqueta
        );

        return new EstilosExcel(
                titulo,
                encabezado,
                dato,
                numero,
                moneda,
                etiquetaResumen
        );
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
                ? fecha.format(FORMATO_FECHA_HORA)
                : "-";
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

    private record EstilosExcel(
            CellStyle titulo,
            CellStyle encabezado,
            CellStyle dato,
            CellStyle numero,
            CellStyle moneda,
            CellStyle etiquetaResumen
    ) {
    }
}