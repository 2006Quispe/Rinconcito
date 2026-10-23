package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.service.reporte.ExportadorReporte;
import com.rinconcito.service.reporte.FormatoReporteNoSoportadoException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExportacionReporteServiceTest {
    static AdminReporteDTO reporte() {
        return new AdminReporteDTO(LocalDate.of(2026,10,1), LocalDate.of(2026,10,22),
                0,0,0,0,0,0,0,List.of(),List.of(),List.of(),List.of());
    }
    static ExportadorReporte exportador(String id, byte[] bytes) {
        return new ExportadorReporte() {
            public String formato() { return id; }
            public String extension() { return "txt"; }
            public String tipoContenido() { return "text/plain"; }
            public byte[] generar(AdminReporteDTO reporte) { return bytes; }
        };
    }
    @Test void admiteOtraEstrategiaSinModificarElCoordinador() {
        var servicio = new ExportacionReporteService(List.of(exportador("nuevo", new byte[]{42})));
        var archivo = servicio.exportar(reporte(), "nuevo");
        assertArrayEquals(new byte[]{42}, archivo.contenido());
        assertEquals("Reporte-Rinconcito-2026-10-01-al-2026-10-22.txt", archivo.nombreArchivo());
        assertEquals("text/plain", archivo.tipoContenido());
    }
    @Test void rechazaFormatosDesconocidosYNulos() {
        var servicio = new ExportacionReporteService(List.of());
        for (String formato : new String[]{null, "", "desconocido"}) {
            assertThrows(FormatoReporteNoSoportadoException.class, () -> servicio.exportar(reporte(), formato));
        }
        assertThrows(NullPointerException.class, () -> servicio.exportar(null, "pdf"));
    }
    @Test void rechazaRegistroAmbiguoOVacio() {
        var exportador = exportador("pdf", new byte[]{1});
        assertThrows(IllegalStateException.class, () -> new ExportacionReporteService(List.of(exportador, exportador)));
        assertThrows(IllegalStateException.class, () -> new ExportacionReporteService(List.of(exportador(" ", new byte[]{1}))));
    }
    @Test void noDevuelveExitoConContenidoVacioONulo() {
        for (byte[] bytes : new byte[][]{null, new byte[0]}) {
            var servicio = new ExportacionReporteService(List.of(exportador("prueba", bytes)));
            assertThrows(IllegalStateException.class, () -> servicio.exportar(reporte(), "prueba"));
        }
    }
}
