package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoWhatsappItem;
import com.ensambla.backend.domain.dto.whatsapp.GenerarCodigoWhatsappRequest;
import com.ensambla.backend.domain.mapper.whatsapp.GenerarCodigoWhatsappMapper;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.VinculoWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GenerarCodigoWhatsappUseCase {

    private final NegocioService negocioService;

    private final VinculoWhatsappService vinculoWhatsappService;

    private final GenerarCodigoWhatsappMapper generarCodigoWhatsappMapper;

    private final Clock clock;

    public CodigoWhatsappItem execute(GenerarCodigoWhatsappRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        VinculoWhatsapp vinculo = vinculoWhatsappService.generarCodigo(negocioId, OffsetDateTime.now(clock));
        return generarCodigoWhatsappMapper.toResponse(vinculo);
    }
}
