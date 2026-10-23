package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.entity.Pago;
import com.rinconcito.service.reporte.ExportadorReporte;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportadoresReporteTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ambosFormatosConservanElContratoConYSinOperaciones(boolean conOperaciones) throws Exception {
        Pago pago = new Pago();
        pago.setIdPago(1);
        pago.setMonto(137);
        pago.setEstado(Pago.EstadoPago.APROBADO);
        pago.setMetodoPago(Pago.MetodoPago.EFECTIVO);
        pago.setFechaPago(LocalDateTime.of(2026,10,22,12,0));
        var operaciones = conOperaciones ? List.of(pago) : List.<Pago>of();
        long total = conOperaciones ? 137 : 0;
        var reporte = new AdminReporteDTO(LocalDate.of(2026,10,1), LocalDate.of(2026,10,22),
                total, operaciones.size(), operaciones.size(),0,0,0,0,
                List.of(),List.of(),List.of(),operaciones);
        var configuracion = mock(ConfiguracionRestauranteService.class);
        List<ExportadorReporte> exportadores = List.of(new AdminReportePdfService(configuracion), new AdminReporteExcelService());
        for (ExportadorReporte exportador : exportadores) {
            byte[] contenido = exportador.generar(reporte);
            assertTrue(contenido.length > 0);
            if (exportador.formato().equals("pdf")) {
                assertEquals("application/pdf", exportador.tipoContenido());
                assertEquals("pdf", exportador.extension());
                try (var pdf = Loader.loadPDF(contenido)) {
                    String texto = new PDFTextStripper().getText(pdf);
                    assertTrue(texto.contains("01/10/2026"));
                    assertTrue(texto.contains("22/10/2026"));
                    assertTrue(texto.contains("Ventas aprobadas S/ " + total + " Total de operaciones"), texto);
                }
            } else {
                assertEquals("xlsx", exportador.extension());
                assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", exportador.tipoContenido());
                try (var excel = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
                    assertEquals(5, excel.getNumberOfSheets());
                    StringBuilder texto = new StringBuilder();
                    boolean tieneTotal = false;
                    for (var hoja : excel) for (var fila : hoja) for (var celda : fila) {
                        texto.append(celda.toString()).append(" ");
                        if (celda.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                                && celda.getNumericCellValue() == total) tieneTotal = true;
                    }
                    assertTrue(texto.toString().contains("01/10/2026"));
                    assertTrue(texto.toString().contains("22/10/2026"));
                    assertTrue(tieneTotal);
                    assertEquals(total, excel.getSheet("Resumen").getRow(3).getCell(1).getNumericCellValue());
                }
            }
            assertEquals(total, reporte.getTotalVentas());
            assertEquals(operaciones, reporte.getOperaciones());
            assertTrue(reporte.getVentasDiarias().isEmpty());
            assertTrue(reporte.getMetodosPago().isEmpty());
            assertTrue(reporte.getProductosVendidos().isEmpty());
            assertEquals(137, pago.getMonto());
            assertEquals(Pago.EstadoPago.APROBADO, pago.getEstado());
            assertEquals(Pago.MetodoPago.EFECTIVO, pago.getMetodoPago());
        }
    }
}
