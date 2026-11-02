package com.ensambla.backend.domain.useCase.reporte.exportarResumen;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

/** El PDF del resumen del negocio y el nombre con el que se descarga. */
public record ExportarResumenResponse(byte[] contenido, String nombreArchivo) {

    /** El nombre es {@code resumen-<sistema>-<aaaammdd>.pdf}, sin tildes ni espacios. */
    public static ExportarResumenResponse de(byte[] contenido, String nombreSistema, LocalDate hoy) {
        String limpio = Normalizer.normalize(nombreSistema, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        String nombre = limpio.isEmpty() ? "resumen" : "resumen-" + limpio;
        return new ExportarResumenResponse(contenido, nombre + "-" + hoy.toString().replace("-", "") + ".pdf");
    }
}
