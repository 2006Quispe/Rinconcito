package com.rinconcito.service.reporte;

import com.rinconcito.dto.AdminReporteDTO;

/**
 * Exporta un reporte valido (fechas resueltas y listas no nulas), incluso sin
 * operaciones. No modifica los datos y conserva periodo e importes compartidos.
 * Los metadatos son estables y coherentes con el documento generado.
 * generar devuelve bytes legibles y no vacios; un fallo se comunica mediante
 * excepcion, conservando la causa en errores de entrada/salida.
 */
public interface ExportadorReporte {
    String formato();
    String extension();
    String tipoContenido();
    byte[] generar(AdminReporteDTO reporte);
}
