package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;

public interface MensajeWhatsappRepository {

    MensajeWhatsapp save(MensajeWhatsapp mensaje);

    boolean existsByIdExterno(String idExterno);
}
