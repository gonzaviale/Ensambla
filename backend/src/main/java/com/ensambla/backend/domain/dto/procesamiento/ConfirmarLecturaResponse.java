package com.ensambla.backend.domain.dto.procesamiento;

import java.util.UUID;

public record ConfirmarLecturaResponse(UUID proyectoId, UUID bloqueId, int registros) {
}
