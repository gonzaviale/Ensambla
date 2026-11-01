package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPruebaPremiumJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PruebaPremiumRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PruebaPremiumRepositoryImpl implements PruebaPremiumRepository {

    private final IPruebaPremiumJpaRepository pruebaPremiumJpaRepository;

    private final PruebaPremiumRepositoryMapper pruebaPremiumRepositoryMapper;

    @Override
    public Optional<PruebaPremium> findByNegocioId(NegocioId negocioId) {
        return pruebaPremiumJpaRepository.findById(negocioId.valor()).map(pruebaPremiumRepositoryMapper::toDomain);
    }

    @Override
    public PruebaPremium save(PruebaPremium prueba) {
        return pruebaPremiumRepositoryMapper.toDomain(pruebaPremiumJpaRepository.saveAndFlush(
                pruebaPremiumRepositoryMapper.toEntity(prueba)));
    }

    @Override
    public List<NegocioId> findNegociosVencidos(OffsetDateTime ahora) {
        return pruebaPremiumJpaRepository.findNegociosVencidos(ahora).stream().map(NegocioId::de).toList();
    }
}
