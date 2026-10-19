package com.ensambla.backend.infrastructure.presentation.dto.sami;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Un turno de la charla con Sami en el taller.
 *
 * @param mensaje   lo que la persona escribió o eligió en la botonera
 * @param estado    el sistema que se está armando, resumido (sin filas ni datos de ejemplo)
 * @param historial los últimos mensajes de la charla, del más viejo al más nuevo
 */
public record ConversarTallerRequest(
        @NotBlank @Size(max = 2500) String mensaje,
        @NotNull @Valid Estado estado,
        @Size(max = 10) List<@Valid Mensaje> historial
) {

    public record Estado(
            @Size(max = 120) String nombre,
            @NotBlank @Size(max = 20) String distribucion,
            @NotNull @Size(max = 30) List<@Valid Pieza> piezas
    ) {
    }

    public record Pieza(
            @Size(max = 80) String id,
            @NotBlank @Size(max = 20) String tipo,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 120) String seccion,
            @NotNull @Size(max = 40) List<@Valid Campo> campos
    ) {
    }

    public record Campo(@NotBlank @Size(max = 120) String etiqueta, @NotBlank @Size(max = 20) String tipo) {
    }

    public record Mensaje(
            @NotBlank @Pattern(regexp = "sami|you") String quien,
            @NotBlank @Size(max = 1000) String texto
    ) {
    }
}
