package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import java.util.UUID;

public record EliminarCampoRequest(UUID usuarioId, UUID campoId) {
}
