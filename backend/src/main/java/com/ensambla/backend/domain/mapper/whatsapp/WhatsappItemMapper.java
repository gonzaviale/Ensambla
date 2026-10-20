package com.ensambla.backend.domain.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoVinculacionItem;
import com.ensambla.backend.domain.dto.whatsapp.VinculoWhatsappItem;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

public class WhatsappItemMapper {

    public CodigoVinculacionItem toItem(CodigoVinculacion codigo, String numeroBot) {
        return new CodigoVinculacionItem(codigo.getCodigo(), codigo.getExpiraEn(), numeroBot);
    }

    public VinculoWhatsappItem toItem(VinculoWhatsapp vinculo) {
        return new VinculoWhatsappItem(vinculo.getTelefono(), vinculo.getVinculadoEn());
    }
}
