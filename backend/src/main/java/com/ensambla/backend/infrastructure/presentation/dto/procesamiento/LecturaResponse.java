package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura.PropuestaItem;

public record LecturaResponse(LoteId loteId, String estado, String motivoError, int intentos, PropuestaItem propuesta) {
}
