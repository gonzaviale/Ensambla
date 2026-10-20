package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import java.time.OffsetDateTime;

public record VinculoWhatsappResponse(String telefono, OffsetDateTime vinculadoEn) {
}
