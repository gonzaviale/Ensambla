package com.ensambla.backend.infrastructure.presentation.dto.sami;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespuestaSamiRequest(
        @NotBlank @Size(max = 60) String preguntaId,
        @NotBlank @Size(max = 60) String opcionId
) {
}
