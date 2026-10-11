package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import java.util.UUID;

public record ConfirmacionResponse(UUID proyectoId, UUID bloqueId, int registros) {
}
