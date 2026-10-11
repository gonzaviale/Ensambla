package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.PropuestaItem;

import java.util.UUID;

public record LecturaResponse(UUID loteId, String estado, String motivoError, int intentos, PropuestaItem propuesta) {
}
