package com.ensambla.backend.infrastructure.presentation.dto.sami;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * @param plantillaId plantilla elegida por Sami
 * @param nombre      opcional; sin él, el proyecto se llama como la plantilla
 * @param columnas    columnas extra que la persona dejó en la propuesta
 */
public record CrearProyectoSamiRequest(
        @NotNull UUID plantillaId,
        @Size(max = 100) String nombre,
        @NotNull @Size(max = 10) List<@Valid ColumnaSamiRequest> columnas
) {

    public record ColumnaSamiRequest(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 20) String tipo,
            @Size(max = 50) List<@Size(max = 120) String> opciones
    ) {
    }
}
