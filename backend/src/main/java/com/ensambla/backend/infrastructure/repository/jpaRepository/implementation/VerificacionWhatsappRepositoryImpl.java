package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVerificacionWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VerificacionWhatsappRepositoryImpl implements VerificacionWhatsappRepository {

    private final IVerificacionWhatsappJpaRepository jpa;
    private final WhatsappRepositoryMapper mapper;

    @Override
    public VerificacionWhatsapp save(VerificacionWhatsapp verificacion) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(verificacion)));
    }

    @Override
    public Optional<VerificacionWhatsapp> findByUsuarioId(UsuarioId usuarioId) {
        return jpa.findByUsuarioId(usuarioId.valor()).map(mapper::toDomain);
    }

    @Override
    public void deleteByUsuarioId(UsuarioId usuarioId) {
        jpa.deleteByUsuarioId(usuarioId.valor());
    }

    @Override
    public long contarEnviosRecientesA(String telefono, OffsetDateTime desde) {
        return jpa.countByTelefonoAndEnviadoEnGreaterThanEqual(telefono, desde);
    }
}
