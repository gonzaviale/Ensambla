package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.VinculoWhatsappRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VinculoWhatsappRepositoryImpl implements VinculoWhatsappRepository {

    private final IVinculoWhatsappJpaRepository vinculoWhatsappJpaRepository;

    private final VinculoWhatsappRepositoryMapper vinculoWhatsappRepositoryMapper;

    @Override
    public VinculoWhatsapp save(VinculoWhatsapp vinculo) {
        return vinculoWhatsappRepositoryMapper.toDomain(
                vinculoWhatsappJpaRepository.saveAndFlush(vinculoWhatsappRepositoryMapper.toEntity(vinculo)));
    }

    @Override
    public Optional<VinculoWhatsapp> findByNegocioId(UUID negocioId) {
        return vinculoWhatsappJpaRepository.findByNegocioId(negocioId)
                .map(vinculoWhatsappRepositoryMapper::toDomain);
    }

    @Override
    public Optional<VinculoWhatsapp> findByCodigo(String codigo) {
        return vinculoWhatsappJpaRepository.findByCodigo(codigo)
                .map(vinculoWhatsappRepositoryMapper::toDomain);
    }

    @Override
    public Optional<VinculoWhatsapp> findByTelefono(String telefono) {
        return vinculoWhatsappJpaRepository.findByTelefono(telefono)
                .map(vinculoWhatsappRepositoryMapper::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return vinculoWhatsappJpaRepository.existsByCodigo(codigo);
    }
}
