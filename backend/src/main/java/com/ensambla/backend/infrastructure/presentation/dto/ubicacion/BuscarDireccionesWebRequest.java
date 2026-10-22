package com.ensambla.backend.infrastructure.presentation.dto.ubicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuscarDireccionesWebRequest(
        @NotBlank(message = "Escribí la dirección de tu comercio")
        @Size(min = 5, max = 200, message = "La dirección debe tener entre 5 y 200 caracteres") String consulta) {
}
