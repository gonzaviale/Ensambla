package com.ensambla.backend.infrastructure.presentation.dto.sami;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param texto      lo que la persona escribió o dictó sobre su negocio
 * @param respuestas opciones que fue eligiendo en las preguntas anteriores de Sami, en orden
 */
public record ConversarSamiRequest(
        @NotBlank @Size(max = 2500) String texto,
        @Size(max = 6) List<@Valid RespuestaSamiRequest> respuestas
) {
}
