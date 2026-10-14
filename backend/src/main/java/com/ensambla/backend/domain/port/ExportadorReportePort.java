package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.reporte.TablaReporte;

/** Escribe un reporte en un archivo de planilla (XLSX) para descargar. */
public interface ExportadorReportePort {

    byte[] exportar(TablaReporte tabla);
}
