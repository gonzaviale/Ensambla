package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.WhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VinculoWhatsappRepositoryImpl implements VinculoWhatsappRepository {

    private final IVinculoWhatsappJpaRepository jpa;
    private final WhatsappRepositoryMapper mapper;

    @Override
    public VinculoWhatsapp save(VinculoWhatsapp vinculo) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(vinculo)));
    }

    @Override
    public Optional<VinculoWhatsapp> findByUsuarioId(UUID usuarioId) {
        return jpa.findByUsuarioId(usuarioId).map(mapper::toDomain);
    }

    @Override
    public Optional<VinculoWhatsapp> findByTelefono(String telefono) {
        return jpa.findByTelefono(telefono).map(mapper::toDomain);
    }

    @Override
    public void deleteByUsuarioId(UUID usuarioId) {
        jpa.deleteByUsuarioId(usuarioId);
    }
}
