package com.ensambla.backend.infrastructure.presentation.dto.preferencia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record PreferenciasWebRequest(
        @NotNull @Size(max = 200) List<@NotNull @Size(max = 200) String> descartadas,
        @NotNull Map<@NotNull String, @NotNull Boolean> grupos,
        @NotNull Boolean bienvenidaVista,
        @Size(max = 40) String nombre) {
}
