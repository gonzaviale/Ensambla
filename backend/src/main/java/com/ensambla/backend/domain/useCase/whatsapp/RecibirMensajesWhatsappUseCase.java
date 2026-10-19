package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.mapper.whatsapp.RecibirMensajesWhatsappMapper;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntranteWhatsapp;
import com.ensambla.backend.domain.service.whatsapp.MensajeWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class RecibirMensajesWhatsappUseCase {

    private final MensajeWhatsappService mensajeWhatsappService;

    private final RecibirMensajesWhatsappMapper recibirMensajesWhatsappMapper;

    private final Clock clock;

    public void execute(RecibirMensajesWhatsappRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        List<MensajeEntranteWhatsapp> mensajes = recibirMensajesWhatsappMapper.toEntrantes(request);
        mensajes.forEach(mensaje -> mensajeWhatsappService.recibir(mensaje, ahora));
    }
}
