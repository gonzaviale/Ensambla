package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

public record ConfirmacionResponse(ProyectoId proyectoId, BloqueId bloqueId, int registros) {
}
