package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPruebaPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PruebaPremiumRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PruebaPremiumRepositoryImpl implements PruebaPremiumRepository {

    private final IPruebaPremiumJpaRepository pruebaPremiumJpaRepository;

    private final PruebaPremiumRepositoryMapper pruebaPremiumRepositoryMapper;

    @Override
    public Optional<PruebaPremium> findByNegocioId(UUID negocioId) {
        return pruebaPremiumJpaRepository.findById(negocioId).map(pruebaPremiumRepositoryMapper::toDomain);
    }

    @Override
    public PruebaPremium save(PruebaPremium prueba) {
        return pruebaPremiumRepositoryMapper.toDomain(pruebaPremiumJpaRepository.saveAndFlush(
                pruebaPremiumRepositoryMapper.toEntity(prueba)));
    }

    @Override
    public List<UUID> findNegociosVencidos(OffsetDateTime ahora) {
        return pruebaPremiumJpaRepository.findNegociosVencidos(ahora);
    }
}
