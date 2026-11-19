package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;

/**
 * Una métrica que el catálogo del rubro le sugiere al cuaderno, con lo que daría hoy.
 *
 * @param yaLaTenes si ya está a la vista (guardada o en el panel de ventas): se muestra pero no se puede agregar
 */
public record SugeridaItem(DefinicionItem definicion, ResultadoItem resultado, boolean yaLaTenes) {

    public static SugeridaItem de(DefinicionMetrica definicion, ResultadoMetrica resultado, boolean yaLaTenes) {
        return new SugeridaItem(DefinicionItem.de(definicion), ResultadoItem.de(resultado), yaLaTenes);
    }
}
