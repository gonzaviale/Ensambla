package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.CodigoVinculacionItem;
import com.ensambla.backend.domain.mapper.whatsapp.WhatsappItemMapper;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GenerarCodigoVinculacionUseCase {

    private final NegocioService negocioService;
    private final VinculacionWhatsappService vinculacionService;
    private final MensajeriaWhatsappPort mensajeria;
    private final WhatsappItemMapper mapper;
    private final Clock clock;

    public CodigoVinculacionItem execute(UUID usuarioId) {
        negocioService.obtenerDelUsuario(usuarioId);
        return mapper.toItem(vinculacionService.generarCodigo(usuarioId, OffsetDateTime.now(clock)),
                mensajeria.numeroBot());
    }
}
