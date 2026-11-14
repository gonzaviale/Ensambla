package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;

/** Una métrica que el catálogo del rubro le sugiere al cuaderno, con lo que daría hoy. */
public record SugeridaItem(DefinicionItem definicion, ResultadoItem resultado) {

    public static SugeridaItem de(DefinicionMetrica definicion, ResultadoMetrica resultado) {
        return new SugeridaItem(DefinicionItem.de(definicion), ResultadoItem.de(resultado));
    }
}
