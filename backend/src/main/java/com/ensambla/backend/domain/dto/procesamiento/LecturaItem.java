package com.ensambla.backend.domain.dto.procesamiento;

import java.util.UUID;

public record LecturaItem(UUID loteId, String estado, String motivoError, int intentos, PropuestaItem propuesta) {
}
