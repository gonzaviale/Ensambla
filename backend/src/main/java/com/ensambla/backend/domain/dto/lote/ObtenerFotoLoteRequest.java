package com.ensambla.backend.domain.dto.lote;

import java.util.UUID;

public record ObtenerFotoLoteRequest(UUID usuarioId, UUID loteId, UUID fotoId) {
}
