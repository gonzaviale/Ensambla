package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICobroQrJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CobroQrRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CobroQrRepositoryImpl implements CobroQrRepository {

    private final ICobroQrJpaRepository cobroQrJpaRepository;
    private final CobroQrRepositoryMapper cobroQrRepositoryMapper;

    @Override
    public Optional<CobroQr> findByIdAndNegocioId(CobroQrId id, NegocioId negocioId) {
        return cobroQrJpaRepository.findByIdAndNegocioId(id.valor(), negocioId.valor())
                .map(cobroQrRepositoryMapper::toDomain);
    }

    @Override
    public CobroQr save(CobroQr cobro) {
        return cobroQrRepositoryMapper.toDomain(cobroQrJpaRepository.save(cobroQrRepositoryMapper.toEntity(cobro)));
    }
}
