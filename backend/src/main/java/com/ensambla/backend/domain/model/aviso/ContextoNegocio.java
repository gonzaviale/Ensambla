package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.List;

/** Datos del negocio sobre los que las reglas de aviso trabajan en una corrida del motor. */
public record ContextoNegocio(NegocioId negocioId, List<BloqueInventario> inventarios) {
}
