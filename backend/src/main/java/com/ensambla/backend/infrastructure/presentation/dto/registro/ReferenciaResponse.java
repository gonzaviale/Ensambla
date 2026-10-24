package com.ensambla.backend.infrastructure.presentation.dto.registro;

import com.ensambla.backend.domain.model.registro.RegistroId;

/** Una fila de otra tabla tal como se muestra en un campo RELACION. */
public record ReferenciaResponse(RegistroId id, String etiqueta) {
}
