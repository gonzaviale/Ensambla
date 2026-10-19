package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import java.time.OffsetDateTime;

/**
 * @param codigo  lo que la persona tiene que mandar por WhatsApp al número de Ensambla
 * @param venceEn hasta cuándo sirve el código
 */
public record CodigoWhatsappResponse(String codigo, OffsetDateTime venceEn) {
}
