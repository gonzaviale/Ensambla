package com.ensambla.backend.infrastructure.presentation.dto.negocio;

import java.time.OffsetDateTime;

public record NegocioDeLaCuentaWebResponse(
        String id, String nombre, String rubro, boolean activo, OffsetDateTime creadoEn) {
}
