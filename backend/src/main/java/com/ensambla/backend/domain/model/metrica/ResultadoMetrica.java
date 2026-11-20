package com.ensambla.backend.domain.model.metrica;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Lo que da una métrica. Si no se puede calcular, {@code valor} es {@code null} y {@code motivo} dice por qué en
 * palabras de la persona: nunca se devuelve un cero inventado.
 *
 * @param valor el número total, o {@code null} si no se pudo calcular
 * @param filas el reparto (por producto, cliente, categoría o día); vacío si la métrica no se reparte
 * @param motivo por qué no hay número, o {@code null} si lo hay
 */
public record ResultadoMetrica(
        FormatoMetrica formato, BigDecimal valor, List<Fila> filas, String motivo, DeDondeSale deDondeSale) {

    public ResultadoMetrica {
        filas = List.copyOf(Objects.requireNonNullElse(filas, List.of()));
    }

    public static ResultadoMetrica conValor(
            FormatoMetrica formato, BigDecimal valor, List<Fila> filas, DeDondeSale deDondeSale) {
        return new ResultadoMetrica(formato, valor, filas, null, deDondeSale);
    }

    public static ResultadoMetrica sinValor(FormatoMetrica formato, String motivo, DeDondeSale deDondeSale) {
        return new ResultadoMetrica(formato, null, List.of(), motivo, deDondeSale);
    }

    public boolean calculable() {
        return motivo == null;
    }

    /** Una parte del reparto: por ejemplo, lo vendido de "Pan". */
    public record Fila(String etiqueta, BigDecimal valor) {
    }
}
