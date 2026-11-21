package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;

/** La receta de una métrica, lista para mostrar. */
public record DefinicionItem(
        String nombre,
        FuenteMetrica fuente,
        CalculoMetrica calculo,
        AgrupacionMetrica agrupar,
        PeriodoMetrica periodo,
        boolean soloFiado) {

    public static DefinicionItem de(DefinicionMetrica definicion) {
        return new DefinicionItem(
                definicion.nombre(),
                definicion.fuente(),
                definicion.calculo(),
                definicion.agrupar(),
                definicion.periodo(),
                definicion.soloFiado());
    }
}
