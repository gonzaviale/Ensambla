package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** La conversación completa con Sami hasta ahora: el servidor no la guarda. */
public record ProponerObjetivoWebRequest(
        @NotEmpty(message = "Contale a Sami qué querés lograr")
        @Size(max = 20, message = "La conversación es demasiado larga")
        List<@NotNull(message = "El mensaje es obligatorio") @Valid MensajeWebRequest> mensajes) {
}
