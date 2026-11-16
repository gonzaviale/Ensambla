package com.ensambla.backend.infrastructure.presentation.dto.cartel;

import com.ensambla.backend.domain.model.cartel.TipoDeCartel;

public record TextosDeCartelResponse(
        TipoDeCartel plantilla,
        String titular,
        String bajada,
        String textoPosteo,
        boolean redactadoConIa) {
}
