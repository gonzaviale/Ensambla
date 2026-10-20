package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DesvincularWhatsappUseCase {

    private final VinculacionWhatsappService vinculacionService;

    public void execute(UUID usuarioId) {
        vinculacionService.desvincular(usuarioId);
    }
}
