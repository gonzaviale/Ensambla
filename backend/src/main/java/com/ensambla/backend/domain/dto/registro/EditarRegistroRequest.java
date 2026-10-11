package com.ensambla.backend.domain.dto.registro;

import java.util.Map;
import java.util.UUID;

public record EditarRegistroRequest(UUID usuarioId, UUID registroId, Map<String, Object> valores) {
}
