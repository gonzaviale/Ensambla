package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional
    public Optional<MensajeWhatsapp> reclamarSiguiente(OffsetDateTime ahora, Duration vigencia) {
        return jpa.findSiguienteRecibidoBloqueado()
                .map(mapper::toDomain)
                .map(mensaje -> {
                    mensaje.reclamar(ahora, vigencia);
                    return save(mensaje);
                });
    }

    @Override
    public List<MensajeWhatsapp> vencidos(OffsetDateTime ahora) {
        return jpa.findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp.ATENDIENDO, ahora).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
