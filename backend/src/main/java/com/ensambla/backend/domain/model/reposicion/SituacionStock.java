package com.ensambla.backend.domain.model.reposicion;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Por qué hay que reponer un producto: se agotó ({@link Agotado}) o quedó en su mínimo o por debajo
 * ({@link BajoMinimo}). Cada situación decide cómo se la describe y qué tan urgente es; cuánto pedir es la misma
 * cuenta para las dos.
 */
public abstract class SituacionStock {

    private static final BigDecimal PEDIDO_SIN_MINIMO = BigDecimal.TEN;
    private static final BigDecimal MULTIPLO_DEL_MINIMO = BigDecimal.TWO;

    protected final BigDecimal cantidad;
    protected final BigDecimal minimo;

    /** {@code minimo} es {@code null} cuando el producto no tiene un mínimo definido. */
    protected SituacionStock(BigDecimal cantidad, BigDecimal minimo) {
        this.cantidad = cantidad;
        this.minimo = minimo;
    }

    /** La situación del producto, o vacío si no hace falta reponerlo. Un mínimo en cero o negativo no cuenta. */
    public static Optional<SituacionStock> detectar(BigDecimal cantidad, BigDecimal minimoCargado) {
        BigDecimal minimo = minimoDefinido(minimoCargado);
        return Agotado.siCorresponde(cantidad, minimo).or(() -> BajoMinimo.siCorresponde(cantidad, minimo));
    }

    /** Unidades para llegar al doble del mínimo (o a diez si no hay mínimo); nunca menos de una. */
    public static BigDecimal pedidoPara(BigDecimal cantidad, BigDecimal minimo) {
        return objetivoPara(minimo).subtract(cantidad).max(BigDecimal.ONE);
    }

    public final BigDecimal cantidadAPedir() {
        return pedidoPara(cantidad, minimo);
    }

    /** Cómo se lo cuenta a la persona: «Agotado», «Quedan 3 (mínimo 10)». */
    public abstract String descripcion();

    /** Mayor es más urgente: los agotados van antes que los que solo están bajos. */
    public abstract int urgencia();

    protected static String texto(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static BigDecimal objetivoPara(BigDecimal minimo) {
        return minimo == null ? PEDIDO_SIN_MINIMO : minimo.multiply(MULTIPLO_DEL_MINIMO);
    }

    private static BigDecimal minimoDefinido(BigDecimal minimoCargado) {
        return minimoCargado != null && minimoCargado.signum() > 0 ? minimoCargado : null;
    }
}
