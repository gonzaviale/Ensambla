package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import java.time.OffsetDateTime;

/** @param numeroBot número de WhatsApp de Sami al que hay que escribirle; vacío si no se configuró */
public record VinculoWhatsappResponse(String telefono, OffsetDateTime vinculadoEn, String numeroBot) {
}
