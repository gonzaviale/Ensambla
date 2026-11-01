package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** @param telefono el número con código de país; admite +, espacios, guiones y paréntesis */
public record SolicitarVerificacionWhatsappWebRequest(
        @NotBlank(message = "Escribí tu número de WhatsApp")
        @Pattern(regexp = "^[+0-9 ()-]{8,25}$", message = "Escribí el número con código de país, por ejemplo +54 9 11 2233 4455")
        String telefono
) {
}
