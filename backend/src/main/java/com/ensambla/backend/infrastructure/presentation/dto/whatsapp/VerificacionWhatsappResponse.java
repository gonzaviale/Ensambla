package com.ensambla.backend.infrastructure.presentation.dto.whatsapp;

import java.time.OffsetDateTime;

/** El código ya salió por WhatsApp hacia ese número; no vuelve en la respuesta. */
public record VerificacionWhatsappResponse(String telefono, OffsetDateTime expiraEn, OffsetDateTime reenviarDesde) {
}
