package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ISuscripcionJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.SuscripcionRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SuscripcionRepositoryImpl implements SuscripcionRepository {

    private final ISuscripcionJpaRepository suscripcionJpaRepository;

    private final SuscripcionRepositoryMapper suscripcionRepositoryMapper;

    @Override
    public Suscripcion save(Suscripcion suscripcion) {
        return suscripcionRepositoryMapper.toDomain(
                suscripcionJpaRepository.saveAndFlush(suscripcionRepositoryMapper.toEntity(suscripcion)));
    }

    @Override
    public Optional<Suscripcion> findByNegocioId(NegocioId negocioId) {
        return suscripcionJpaRepository.findByNegocioId(negocioId.valor()).map(suscripcionRepositoryMapper::toDomain);
    }
}
