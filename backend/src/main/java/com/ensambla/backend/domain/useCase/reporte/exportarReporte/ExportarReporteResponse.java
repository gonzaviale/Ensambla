package com.ensambla.backend.domain.useCase.reporte.exportarReporte;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

/** El archivo XLSX del reporte y el nombre con el que se descarga. */
public record ExportarReporteResponse(byte[] contenido, String nombreArchivo) {

    /** El nombre es {@code reporte-<tabla>-<aaaammdd>.xlsx}, sin tildes ni espacios. */
    public static ExportarReporteResponse de(byte[] contenido, String nombreBloque, LocalDate hoy) {
        String limpio = Normalizer.normalize(nombreBloque, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        String nombre = limpio.isEmpty() ? "reporte" : "reporte-" + limpio;
        return new ExportarReporteResponse(contenido, nombre + "-" + hoy.toString().replace("-", "") + ".xlsx");
    }
}
