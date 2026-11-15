package com.ensambla.backend.infrastructure.presentation.dto.negocio;

import java.util.List;

public record ListaNegociosWebResponse(
        String activoId, int limite, boolean puedeCrear, List<NegocioDeLaCuentaWebResponse> negocios) {
}
