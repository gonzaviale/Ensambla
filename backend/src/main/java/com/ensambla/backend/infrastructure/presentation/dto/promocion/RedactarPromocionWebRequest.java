package com.ensambla.backend.infrastructure.presentation.dto.promocion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RedactarPromocionWebRequest(
        @NotBlank @Size(max = 40) String canal,
        @NotBlank @Size(max = 40) String clave,
        @NotBlank @Size(max = 120) String titulo,
        @NotNull @Size(max = 200) String gancho,
        @NotEmpty @Size(max = 12) List<@Valid ProductoPromocionWebRequest> productos
) {
}
