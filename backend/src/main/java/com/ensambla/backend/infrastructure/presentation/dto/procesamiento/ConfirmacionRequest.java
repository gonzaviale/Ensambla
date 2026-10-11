package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfirmacionRequest(
        UUID proyectoId,
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombreProyecto,
        UUID bloqueId,
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombreBloque,
        @NotNull(message = "Las columnas son obligatorias")
        @Valid
        List<CampoRequest> campos,
        @NotNull(message = "Las filas son obligatorias")
        @Valid
        List<FilaRequest> filas
) {

    public record CampoRequest(
            @NotBlank(message = "La columna necesita una clave")
            String clave,
            UUID campoId,
            String nombre,
            String tipo,
            List<String> opciones
    ) {
    }

    public record FilaRequest(
            Integer pagina,
            @NotNull(message = "Los valores son obligatorios")
            Map<String, Object> valores
    ) {
    }
}
