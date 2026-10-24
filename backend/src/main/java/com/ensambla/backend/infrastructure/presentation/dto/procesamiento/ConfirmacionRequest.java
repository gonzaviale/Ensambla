package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ConfirmacionRequest(
        ProyectoId proyectoId,
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombreProyecto,
        BloqueId bloqueId,
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
            CampoId campoId,
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
