package com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp;

import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarVinculacionWhatsapp {

    private final VinculoWhatsappRepository vinculoRepository;

    public ConsultarVinculacionWhatsappResponse execute(ConsultarVinculacionWhatsappRequest request) {
        return ConsultarVinculacionWhatsappResponse.de(vinculoRepository.obtenerDelUsuario(request.usuarioId()));
    }
}
