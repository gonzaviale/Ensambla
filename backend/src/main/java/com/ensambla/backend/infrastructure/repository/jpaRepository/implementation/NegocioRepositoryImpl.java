package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NegocioRepositoryImpl implements NegocioRepository {

    private final INegocioJpaRepository negocioJpaRepository;
    private final NegocioRepositoryMapper negocioRepositoryMapper;

    @Override
    public Negocio save(Negocio negocio) {
        return negocioRepositoryMapper.toDomain(
                negocioJpaRepository.saveAndFlush(negocioRepositoryMapper.toEntity(negocio)));
    }
}

