package com.ensambla.backend.domain.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

public class GenerarCodigoWhatsappMapper {

    public CodigoWhatsappItem toResponse(VinculoWhatsapp vinculo) {
        return new CodigoWhatsappItem(vinculo.getCodigo(), vinculo.getCodigoVenceEn());
    }
}
