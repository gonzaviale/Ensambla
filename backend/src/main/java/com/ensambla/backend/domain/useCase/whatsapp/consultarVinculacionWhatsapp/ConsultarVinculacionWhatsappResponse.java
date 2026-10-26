package com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.time.OffsetDateTime;

public record ConsultarVinculacionWhatsappResponse(String telefono, OffsetDateTime vinculadoEn) {

    public static ConsultarVinculacionWhatsappResponse de(VinculoWhatsapp vinculo) {
        return new ConsultarVinculacionWhatsappResponse(vinculo.getTelefono(), vinculo.getVinculadoEn());
    }
}
