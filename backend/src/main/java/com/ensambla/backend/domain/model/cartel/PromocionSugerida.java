package com.ensambla.backend.domain.model.cartel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Algo que conviene promocionar, con sus números calculados por el sistema: de cuánto a cuánto baja el precio, cuánto se
 * ahorra y qué porcentaje es. La IA nunca calcula nada de esto.
 */
public record PromocionSugerida(TipoDeCartel tipo, List<String> productos, BigDecimal precioAnterior,
                                BigDecimal precioNuevo) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public BigDecimal ahorro() {
        return precioAnterior.subtract(precioNuevo);
    }

    /** El descuento en porcentaje entero, redondeado como lo diría una persona: 19,6 % es 20 %. */
    public BigDecimal descuentoPorcentaje() {
        return ahorro().multiply(CIEN).divide(precioAnterior, 0, RoundingMode.HALF_UP);
    }

    /** Los datos con los que se redactan los textos y se arma el cartel de esta promoción. */
    public DatosDePromocion datos() {
        return new DatosDePromocion(List.of(tipo), productos,
                List.of(precioAnterior, precioNuevo, ahorro(), descuentoPorcentaje()));
    }
}
