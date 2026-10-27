package com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.time.OffsetDateTime;

/**
 * El número ya quedó vinculado.
 *
 * @param numeroBot número de WhatsApp de Sami; vacío si no se configuró
 */
public record ConfirmarVerificacionWhatsappResponse(String telefono, OffsetDateTime vinculadoEn, String numeroBot) {

    public static ConfirmarVerificacionWhatsappResponse de(VinculoWhatsapp vinculo, String numeroBot) {
        return new ConfirmarVerificacionWhatsappResponse(vinculo.getTelefono(), vinculo.getVinculadoEn(), numeroBot);
    }
}
