package com.ensambla.backend.domain.dto.lote;

import java.util.UUID;

public record ObtenerLoteRequest(UUID usuarioId, UUID loteId) {
}
