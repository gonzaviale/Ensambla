package com.ensambla.backend.domain.dto.preferencia;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GuardarPreferenciasRequest(
        UUID usuarioId,
        List<String> descartadas,
        Map<String, Boolean> grupos,
        boolean bienvenidaVista,
        String nombre) {
}
