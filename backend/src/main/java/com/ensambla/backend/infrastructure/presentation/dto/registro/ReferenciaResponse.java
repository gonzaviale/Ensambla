package com.ensambla.backend.infrastructure.presentation.dto.registro;

import java.util.UUID;

/** Una fila de otra tabla tal como se muestra en un campo RELACION. */
public record ReferenciaResponse(UUID id, String etiqueta) {
}
