package com.ensambla.backend.infrastructure.presentation.dto.marca;

import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record KitDeMarcaWebRequest(
        @NotNull @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorPrincipal,
        @NotNull @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorSecundario,
        @NotNull Tipografia tipografia,
        @NotNull TonoDeMarca tono) {
}
