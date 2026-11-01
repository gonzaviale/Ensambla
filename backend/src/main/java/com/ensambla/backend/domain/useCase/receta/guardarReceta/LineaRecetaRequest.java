package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

public record LineaRecetaRequest(RegistroId insumoId, BigDecimal cantidad, String unidad) {
}
