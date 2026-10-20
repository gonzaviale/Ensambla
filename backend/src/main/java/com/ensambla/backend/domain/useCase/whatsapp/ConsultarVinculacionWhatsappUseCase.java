package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.VinculoWhatsappItem;
import com.ensambla.backend.domain.mapper.whatsapp.WhatsappItemMapper;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarVinculacionWhatsappUseCase {

    private final VinculacionWhatsappService vinculacionService;
    private final WhatsappItemMapper mapper;

    public VinculoWhatsappItem execute(UUID usuarioId) {
        return mapper.toItem(vinculacionService.obtener(usuarioId));
    }
}
