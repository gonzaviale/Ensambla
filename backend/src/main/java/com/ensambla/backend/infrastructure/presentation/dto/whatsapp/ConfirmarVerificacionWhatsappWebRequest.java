package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** @param codigo los 6 dígitos que llegaron por WhatsApp */
public record ConfirmarVerificacionWhatsappWebRequest(
        @NotBlank(message = "Escribí el código que te llegó")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos")
        String codigo
) {
}
