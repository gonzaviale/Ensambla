package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NegocioRepositoryImpl implements NegocioRepository {

    private final INegocioJpaRepository negocioJpaRepository;

    @Override
    public Optional<UUID> findIdByDuenioId(UUID usuarioId) {
        return negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioId)
                .map(negocio -> negocio.getId());
    }
}

