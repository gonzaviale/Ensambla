package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaRecetaRequest(UUID insumoId, BigDecimal cantidad, String unidad) {
}
