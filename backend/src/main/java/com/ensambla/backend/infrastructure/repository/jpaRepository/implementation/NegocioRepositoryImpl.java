package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NegocioRepositoryImpl implements NegocioRepository {

    private final INegocioJpaRepository jpa;
    private final NegocioRepositoryMapper mapper;

    @Override
    public Optional<Negocio> findByDuenioId(UUID duenioId) {
        return jpa.findByDuenioId(duenioId).map(mapper::toDomain);
    }
}
