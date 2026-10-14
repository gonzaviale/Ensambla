package com.ensambla.backend.domain.dto.campo;

import java.util.List;
import java.util.UUID;

public record CrearCampoRequest(
        UUID usuarioId,
        UUID bloqueId,
        String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        UUID bloqueRelacionadoId,
        String formula
) {
}
