package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

/** Dónde quedó lo que se confirmó y cuántos registros se crearon. */
public record ConfirmacionGuardada(ProyectoId proyectoId, BloqueId bloqueId, int registros) {
}
