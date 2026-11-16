package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

/** Lo que la IA redactó y lo que gastó, para registrarlo. */
public record RedaccionDeCartelIa(TextosDeCartel textos, ConsumoIaSami consumo) {
}
