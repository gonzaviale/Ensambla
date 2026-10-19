package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MensajeWhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MensajeWhatsappRepositoryImpl implements MensajeWhatsappRepository {

    private final IMensajeWhatsappJpaRepository mensajeWhatsappJpaRepository;

    private final MensajeWhatsappRepositoryMapper mensajeWhatsappRepositoryMapper;

    @Override
    public MensajeWhatsapp save(MensajeWhatsapp mensaje) {
        return mensajeWhatsappRepositoryMapper.toDomain(
                mensajeWhatsappJpaRepository.saveAndFlush(mensajeWhatsappRepositoryMapper.toEntity(mensaje)));
    }

    @Override
    public boolean existsByWaMensajeId(String waMensajeId) {
        return mensajeWhatsappJpaRepository.existsByWaMensajeId(waMensajeId);
    }
}
