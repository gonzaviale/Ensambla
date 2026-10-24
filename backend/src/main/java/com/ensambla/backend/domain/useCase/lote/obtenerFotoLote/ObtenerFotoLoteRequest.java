package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import java.util.UUID;

public record ObtenerFotoLoteRequest(UUID usuarioId, UUID loteId, UUID fotoId) {
}
