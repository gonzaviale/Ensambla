package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.model.cartel.TextosDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;

/** Los textos propuestos para el cartel y si los redactó la IA o son los de reserva. */
public record TextosDeCartelItem(TipoDeCartel plantilla, String titular, String bajada, String textoPosteo,
                                 boolean redactadoConIa) {

    public static TextosDeCartelItem de(TextosDeCartel textos, boolean redactadoConIa) {
        return new TextosDeCartelItem(
                textos.plantilla(), textos.titular(), textos.bajada(), textos.textoPosteo(), redactadoConIa);
    }
}
