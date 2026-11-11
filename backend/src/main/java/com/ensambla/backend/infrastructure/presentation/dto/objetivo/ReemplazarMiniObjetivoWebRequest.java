package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import jakarta.validation.constraints.NotNull;

/** El candidato elegido de la lista de alternativas: se manda de vuelta tal cual vino. */
public record ReemplazarMiniObjetivoWebRequest(
        @NotNull(message = "El tipo es obligatorio") TipoMiniObjetivo tipo,
        RegistroId registroId,
        String referencia) {
}
