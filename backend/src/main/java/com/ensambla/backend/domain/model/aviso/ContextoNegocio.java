package com.ensambla.backend.domain.model.aviso;

import java.util.List;
import java.util.UUID;

/** Datos del negocio sobre los que las reglas de aviso trabajan en una corrida del motor. */
public record ContextoNegocio(UUID negocioId, List<BloqueInventario> inventarios) {
}
