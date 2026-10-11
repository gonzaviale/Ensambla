package com.ensambla.backend.domain.dto.registro;

import java.util.Map;
import java.util.UUID;

public record CrearRegistroRequest(UUID usuarioId, UUID bloqueId, Map<String, Object> valores) {
}
