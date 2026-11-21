package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;

import java.util.List;

/**
 * Una métrica guardada en el cuaderno, con su receta, lo que da hoy y cómo se dibuja.
 *
 * @param graficosPosibles los que se le pueden elegir, para ofrecer sólo esos
 */
public record MetricaItem(
        MetricaId id, DefinicionItem definicion, int orden, AutoriaMetrica autoria, ResultadoItem resultado,
        GraficoMetrica grafico, List<GraficoMetrica> graficosPosibles) {

    public static MetricaItem de(Metrica metrica, ResultadoMetrica resultado) {
        return new MetricaItem(
                metrica.getId(),
                DefinicionItem.de(metrica.getDefinicion()),
                metrica.getOrden(),
                metrica.getAutoria(),
                ResultadoItem.de(resultado),
                metrica.getGrafico(),
                metrica.graficosPosibles());
    }
}
