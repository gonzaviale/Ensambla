package com.ensambla.backend.domain.dto.campo;

import java.util.UUID;

public record EliminarCampoRequest(UUID usuarioId, UUID campoId) {
}
