package com.ensambla.backend.domain.dto.reporte;

/** El archivo XLSX del reporte y el nombre con el que se descarga. */
public record ExportarReporteResponse(byte[] contenido, String nombreArchivo) {
}
