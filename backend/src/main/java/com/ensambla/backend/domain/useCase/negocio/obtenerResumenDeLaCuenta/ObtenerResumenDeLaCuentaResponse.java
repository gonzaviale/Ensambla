package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.reporte.PeriodoResumen;

import java.time.LocalDate;
import java.util.List;

/**
 * La vista conjunta de la cuenta: cada negocio con lo comparable entre rubros (ventas, margen, avisos pendientes) y
 * los totales. Los importes van en pesos enteros; {@code negocios} va del más antiguo al más nuevo.
 */
public record ObtenerResumenDeLaCuentaResponse(Periodo periodo, Totales totales, List<NegocioResumido> negocios) {

    public static ObtenerResumenDeLaCuentaResponse de(PeriodoResumen periodo, List<NegocioResumido> negocios) {
        return new ObtenerResumenDeLaCuentaResponse(
                new Periodo(periodo.desde(), periodo.hasta()),
                Totales.de(negocios),
                negocios);
    }

    /** Los días que mide el resumen, los dos incluidos. */
    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    public record Totales(long ventas, long margen, long avisosPendientes) {

        static Totales de(List<NegocioResumido> negocios) {
            return new Totales(
                    negocios.stream().mapToLong(NegocioResumido::ventas).sum(),
                    negocios.stream().mapToLong(NegocioResumido::margen).sum(),
                    negocios.stream().mapToLong(NegocioResumido::avisosPendientes).sum());
        }
    }
}
