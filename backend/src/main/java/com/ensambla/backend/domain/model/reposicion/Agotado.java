package com.ensambla.backend.domain.model.reposicion;

import java.math.BigDecimal;
import java.util.Optional;

/** El producto no tiene unidades. */
public final class Agotado extends SituacionStock {

    private static final int URGENCIA = 2;

    private Agotado(BigDecimal cantidad, BigDecimal minimo) {
        super(cantidad, minimo);
    }

    static Optional<SituacionStock> siCorresponde(BigDecimal cantidad, BigDecimal minimo) {
        return cantidad.signum() <= 0 ? Optional.of(new Agotado(cantidad, minimo)) : Optional.empty();
    }

    @Override
    public String descripcion() {
        return "Agotado";
    }

    @Override
    public int urgencia() {
        return URGENCIA;
    }
}
