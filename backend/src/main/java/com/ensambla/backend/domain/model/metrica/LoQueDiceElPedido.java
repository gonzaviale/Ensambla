package com.ensambla.backend.domain.model.metrica;

import java.util.Optional;

/** Lo que el pedido en palabras dice de cada parte de la receta; lo que calla queda vacío y toma el de siempre. */
record LoQueDiceElPedido(
        FuenteMetrica fuente,
        Optional<CalculoMetrica> calculo,
        Optional<AgrupacionMetrica> agrupacion,
        Optional<PeriodoMetrica> periodo,
        boolean soloFiado) {

    /** Lo que no se dice suma, no reparte y mira el mes. */
    DefinicionMetrica receta(String nombre) {
        return new DefinicionMetrica(nombre, fuente, calculo.orElse(CalculoMetrica.SUMA),
                agrupacion.orElse(AgrupacionMetrica.NINGUNA), periodo.orElseGet(this::elPeriodoDeSiempre), soloFiado);
    }

    /** El stock es lo que hay hoy y lo que te deben se acumula: sin un período dicho, miran todo lo cargado. */
    private PeriodoMetrica elPeriodoDeSiempre() {
        return fuente.tieneFecha() && !soloFiado ? PeriodoMetrica.MES : PeriodoMetrica.TODO;
    }
}
