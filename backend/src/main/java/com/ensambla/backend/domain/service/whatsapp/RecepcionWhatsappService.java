package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecepcionWhatsappService {

    private final MensajeWhatsappRepository mensajeRepository;

    /** Guarda el mensaje en el negocio del vínculo; vacío si ya estaba (el proveedor reintentó el webhook). */
    @Transactional
    public Optional<MensajeWhatsapp> registrar(VinculoWhatsapp vinculo, MensajeEntrante entrante) {
        if (mensajeRepository.existsByIdExterno(entrante.idExterno())) {
            return Optional.empty();
        }
        return Optional.of(mensajeRepository.save(MensajeWhatsapp.recibido(vinculo.getNegocioId(), entrante)));
    }
}
