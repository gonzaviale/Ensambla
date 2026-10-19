package com.ensambla.backend.infrastructure.presentation.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoWhatsappResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WhatsappPresentationMapper {

    public GenerarCodigoWhatsappRequest toGenerarCodigoRequest(UUID usuarioId) {
        return new GenerarCodigoWhatsappRequest(usuarioId);
    }

    public CodigoWhatsappResponse toResponse(CodigoWhatsappItem codigo) {
        return new CodigoWhatsappResponse(codigo.codigo(), codigo.venceEn());
    }
}
