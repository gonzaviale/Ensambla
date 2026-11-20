package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;

import java.util.List;

/**
 * Cómo viene el negocio en los últimos 30 días, en pesos enteros. Sin ventas {@code hayVentas} es falso, los importes
 * son cero y {@code semanas} viene vacío.
 */
public record ObtenerComoVieneElNegocioResponse(
        boolean hayVentas,
        long vendiste,
        long mercaderia,
        long gastos,
        long queda,
        long teDeben,
        List<Semana> semanas) {

    public static ObtenerComoVieneElNegocioResponse de(ComoVieneElNegocio negocio) {
        return new ObtenerComoVieneElNegocioResponse(
                negocio.hayVentas(),
                negocio.vendiste(),
                negocio.mercaderia(),
                negocio.gastos(),
                negocio.queda(),
                negocio.teDeben(),
                negocio.semanas().stream().map(Semana::de).toList());
    }

    /** El resultado de los 30 días que terminan esa semana. */
    public record Semana(String semana, long queda) {

        static Semana de(ComoVieneElNegocio.Semana semana) {
            return new Semana(semana.semana(), semana.queda());
        }
    }
}
