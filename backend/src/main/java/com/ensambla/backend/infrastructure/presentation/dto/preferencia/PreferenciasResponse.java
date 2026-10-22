package com.ensambla.backend.infrastructure.presentation.dto.preferencia;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreferenciasResponse(
        List<String> descartadas,
        Map<String, Boolean> grupos,
        boolean bienvenidaVista,
        String nombre) {
}
