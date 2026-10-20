package com.ensambla.backend.infrastructure.presentation.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoVinculacionItem;
import com.ensambla.backend.domain.dto.whatsapp.VinculoWhatsappItem;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoVinculacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.VinculoWhatsappResponse;
import org.springframework.stereotype.Component;

@Component
public class WhatsappPresentationMapper {

    public CodigoVinculacionResponse toResponse(CodigoVinculacionItem item) {
        return new CodigoVinculacionResponse(item.codigo(), item.expiraEn(), item.numeroBot());
    }

    public VinculoWhatsappResponse toResponse(VinculoWhatsappItem item) {
        return new VinculoWhatsappResponse(item.telefono(), item.vinculadoEn());
    }
}
