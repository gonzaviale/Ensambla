package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import java.time.OffsetDateTime;

public record CodigoVinculacionResponse(String codigo, OffsetDateTime expiraEn, String numeroBot) {
}
