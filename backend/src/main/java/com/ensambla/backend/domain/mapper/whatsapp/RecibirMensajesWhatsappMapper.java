package com.ensambla.backend.domain.mapper.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntranteWhatsapp;

import java.util.List;

public class RecibirMensajesWhatsappMapper {

    public List<MensajeEntranteWhatsapp> toEntrantes(RecibirMensajesWhatsappRequest request) {
        return request.mensajes().stream()
                .map(this::toEntrante)
                .toList();
    }

    private MensajeEntranteWhatsapp toEntrante(MensajeEntrante mensaje) {
        return new MensajeEntranteWhatsapp(
                mensaje.waMensajeId(),
                mensaje.telefono(),
                TipoMensajeWhatsapp.valueOf(mensaje.tipo()),
                mensaje.texto(),
                mensaje.mediaId(),
                mensaje.recibidoEn());
    }
}
