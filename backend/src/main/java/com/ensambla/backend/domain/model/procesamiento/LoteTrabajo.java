package com.ensambla.backend.domain.model.procesamiento;

import java.util.UUID;

public record LoteTrabajo(UUID id, UUID negocioId, UUID bloqueDestinoId, UUID reclamoId, int intentos) {
}
