package com.ensambla.backend.infrastructure.presentation.dto.marca;

import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;

public record KitDeMarcaResponse(
        String colorPrincipal,
        String colorSecundario,
        Tipografia tipografia,
        TonoDeMarca tono,
        boolean tieneLogo) {
}
