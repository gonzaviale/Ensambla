package com.ensambla.backend.infrastructure.presentation.dto.campo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body de crear y de editar un campo. Editar reemplaza toda la configuración. El tipo, las opciones y el valor
 * inicial los valida el dominio, que responde 400 con un mensaje por dato.
 */
public record CampoWebRequest(
        @NotBlank @Size(max = 120) String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones
) {
}
