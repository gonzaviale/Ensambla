package com.ensambla.backend.domain.model.reposicion;

import java.math.BigDecimal;
import java.util.Optional;

/** Quedan unidades, pero no más que el mínimo que definió el negocio. */
public final class BajoMinimo extends SituacionStock {

    private static final int URGENCIA = 1;

    private BajoMinimo(BigDecimal cantidad, BigDecimal minimo) {
        super(cantidad, minimo);
    }

    static Optional<SituacionStock> siCorresponde(BigDecimal cantidad, BigDecimal minimo) {
        return superaElMinimo(cantidad, minimo) ? Optional.empty() : Optional.of(new BajoMinimo(cantidad, minimo));
    }

    @Override
    public String descripcion() {
        return "Quedan " + texto(cantidad) + " (mínimo " + texto(minimo) + ")";
    }

    @Override
    public int urgencia() {
        return URGENCIA;
    }

    private static boolean superaElMinimo(BigDecimal cantidad, BigDecimal minimo) {
        return minimo == null || cantidad.compareTo(minimo) > 0;
    }
}
