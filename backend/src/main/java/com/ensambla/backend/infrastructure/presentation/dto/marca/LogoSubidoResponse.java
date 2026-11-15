package com.ensambla.backend.infrastructure.presentation.dto.marca;

public record LogoSubidoResponse(
        KitDeMarcaResponse kit,
        String colorPrincipalSugerido,
        String colorSecundarioSugerido) {
}
