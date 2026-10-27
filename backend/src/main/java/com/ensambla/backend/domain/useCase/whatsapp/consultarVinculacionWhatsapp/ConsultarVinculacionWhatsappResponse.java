package com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.time.OffsetDateTime;

/** @param numeroBot número de WhatsApp de Sami; vacío si no se configuró */
public record ConsultarVinculacionWhatsappResponse(String telefono, OffsetDateTime vinculadoEn, String numeroBot) {

    public static ConsultarVinculacionWhatsappResponse de(VinculoWhatsapp vinculo, String numeroBot) {
        return new ConsultarVinculacionWhatsappResponse(vinculo.getTelefono(), vinculo.getVinculadoEn(), numeroBot);
    }
}
