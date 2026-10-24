package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import java.util.UUID;

public record ObtenerLoteRequest(UUID usuarioId, UUID loteId) {
}
