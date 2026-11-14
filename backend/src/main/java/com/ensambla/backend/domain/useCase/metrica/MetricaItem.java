package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;

/** Una métrica guardada en el cuaderno, con su receta y lo que da hoy. */
public record MetricaItem(
        MetricaId id, DefinicionItem definicion, int orden, AutoriaMetrica autoria, ResultadoItem resultado) {

    public static MetricaItem de(Metrica metrica, ResultadoMetrica resultado) {
        return new MetricaItem(
                metrica.getId(),
                DefinicionItem.de(metrica.getDefinicion()),
                metrica.getOrden(),
                metrica.getAutoria(),
                ResultadoItem.de(resultado));
    }
}
