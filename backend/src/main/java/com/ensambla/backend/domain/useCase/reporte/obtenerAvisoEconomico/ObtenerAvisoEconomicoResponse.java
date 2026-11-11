package com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico;

import com.ensambla.backend.domain.model.reporte.AvisoEconomico;

import java.math.BigDecimal;
import java.util.List;

/**
 * El aviso económico del inicio.
 *
 * @param aviso {@code null} si todavía no hay una semana anterior con ventas para comparar
 */
public record ObtenerAvisoEconomicoResponse(Aviso aviso) {

    public static ObtenerAvisoEconomicoResponse de(AvisoEconomico aviso) {
        return new ObtenerAvisoEconomicoResponse(aviso == null ? null : Aviso.de(aviso));
    }

    /** Ver {@link AvisoEconomico}: los niveles y la tendencia van como texto. */
    public record Aviso(
            String prioridad,
            String tendencia,
            String categoria,
            String titulo,
            String explicacion,
            BigDecimal variacionVentas,
            BigDecimal variacionResultadoBruto,
            List<String> productos,
            String confianza) {

        static Aviso de(AvisoEconomico aviso) {
            return new Aviso(
                    aviso.prioridad().name(),
                    aviso.tendencia().name(),
                    aviso.categoria(),
                    aviso.titulo(),
                    aviso.explicacion(),
                    aviso.variacionVentas(),
                    aviso.variacionResultadoBruto(),
                    aviso.productos(),
                    aviso.confianza().name());
        }
    }
}
