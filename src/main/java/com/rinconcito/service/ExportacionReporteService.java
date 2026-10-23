package com.rinconcito.service;

import com.rinconcito.dto.AdminReporteDTO;
import com.rinconcito.dto.ArchivoReporteDTO;
import com.rinconcito.service.reporte.ExportadorReporte;
import com.rinconcito.service.reporte.FormatoReporteNoSoportadoException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExportacionReporteService {
    private final Map<String, ExportadorReporte> exportadores;

    public ExportacionReporteService(List<ExportadorReporte> implementaciones) {
        Map<String, ExportadorReporte> registro = new HashMap<>();
        for (ExportadorReporte exportador : implementaciones) {
            String formato = exportador.formato();
            if (formato == null || formato.isBlank()) {
                throw new IllegalStateException("Identificador de exportador vacio.");
            }
            if (registro.putIfAbsent(formato, exportador) != null) {
                throw new IllegalStateException("Identificador de exportador duplicado: " + formato);
            }
        }
        exportadores = Map.copyOf(registro);
    }

    public ArchivoReporteDTO exportar(AdminReporteDTO reporte, String formato) {
        Objects.requireNonNull(reporte, "El reporte no puede ser nulo.");
        ExportadorReporte exportador = formato == null ? null : exportadores.get(formato);
        if (exportador == null) {
            throw new FormatoReporteNoSoportadoException();
        }
        byte[] contenido = exportador.generar(reporte);
        if (contenido == null || contenido.length == 0) {
            throw new IllegalStateException("El exportador no genero contenido.");
        }
        String nombre = "Reporte-Rinconcito-" + reporte.getDesde() + "-al-"
                + reporte.getHasta() + "." + exportador.extension();
        return new ArchivoReporteDTO(contenido, nombre, exportador.tipoContenido());
    }
}
