package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MensajeWhatsappRepositoryImpl implements MensajeWhatsappRepository {

    private final IMensajeWhatsappJpaRepository jpa;
    private final WhatsappRepositoryMapper mapper;

    @Override
    public MensajeWhatsapp save(MensajeWhatsapp mensaje) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(mensaje)));
    }

    @Override
    public boolean existsByIdExterno(String idExterno) {
        return jpa.existsByIdExterno(idExterno);
    }
}
