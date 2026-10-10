package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.BloqueRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BloqueRepositoryImpl implements BloqueRepository {

    private final IBloqueJpaRepository jpa;
    private final BloqueRepositoryMapper mapper;

    @Override
    public Optional<Bloque> findByIdAndNegocioId(UUID id, UUID negocioId) {
        return jpa.findByIdAndNegocioId(id, negocioId).map(mapper::toDomain);
    }

    @Override
    public Bloque save(Bloque bloque) {
        return mapper.toDomain(jpa.save(mapper.toEntity(bloque)));
    }

    @Override
    public void deleteByIdAndNegocioId(UUID id, UUID negocioId) {
        jpa.deleteByIdAndNegocioId(id, negocioId);
    }
}
