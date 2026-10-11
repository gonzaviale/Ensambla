package com.ensambla.backend.domain.model.procesamiento;

import java.util.UUID;

/** Dónde quedó lo que se confirmó y cuántos registros se crearon. */
public record ConfirmacionGuardada(UUID proyectoId, UUID bloqueId, int registros) {
}
