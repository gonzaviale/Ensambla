package com.ensambla.backend.domain.dto.plan;

import java.util.List;
import java.util.UUID;

public record DesbloquearContenidosRequest(UUID usuarioId, String tipo, List<String> claves) {
}
