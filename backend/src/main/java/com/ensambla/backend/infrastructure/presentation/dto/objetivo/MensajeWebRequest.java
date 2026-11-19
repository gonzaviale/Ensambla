package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.ConversacionObjetivo;
import com.ensambla.backend.domain.model.objetivo.RolMensaje;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un mensaje de la conversación con Sami: quién lo escribió y qué dice. */
public record MensajeWebRequest(
        @NotNull(message = "Indicá quién escribió el mensaje") RolMensaje rol,
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = ConversacionObjetivo.LARGO_MAXIMO_MENSAJE,
                message = "El mensaje puede tener hasta 500 caracteres") String texto) {
}
