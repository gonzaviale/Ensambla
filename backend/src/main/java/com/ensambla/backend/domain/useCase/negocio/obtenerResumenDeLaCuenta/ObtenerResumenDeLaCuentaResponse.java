package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.reporte.PeriodoResumen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * La vista conjunta de la cuenta ({@code docs/negocios-de-la-cuenta.md}): el período, los totales y cada negocio, del
 * más antiguo al más nuevo.
 */
public record ObtenerResumenDeLaCuentaResponse(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        Total total,
        List<NegocioResumido> negocios) {

    public static ObtenerResumenDeLaCuentaResponse de(PeriodoResumen periodo, List<NegocioResumido> negocios) {
        return new ObtenerResumenDeLaCuentaResponse(
                periodo.desde(), periodo.hasta(), periodo.dias(), Total.de(negocios), negocios);
    }

    /** {@code negocios} cuenta todos los de la cuenta; los importes suman sólo lo que tiene valor. */
    public record Total(
            int negocios,
            BigDecimal vendiste,
            BigDecimal gastos,
            BigDecimal queda,
            BigDecimal teDeben,
            long avisosPendientes) {

        static Total de(List<NegocioResumido> negocios) {
            return new Total(
                    negocios.size(),
                    suma(negocios, NegocioResumido::vendiste),
                    suma(negocios, NegocioResumido::gastos),
                    suma(negocios, NegocioResumido::queda),
                    suma(negocios, NegocioResumido::teDeben),
                    negocios.stream().mapToLong(NegocioResumido::avisosPendientes).sum());
        }

        private static BigDecimal suma(List<NegocioResumido> negocios, Function<NegocioResumido, BigDecimal> metrica) {
            return negocios.stream().map(metrica).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
