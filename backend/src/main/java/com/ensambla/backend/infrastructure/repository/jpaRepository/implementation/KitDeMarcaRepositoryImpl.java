package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IKitDeMarcaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.KitDeMarcaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KitDeMarcaRepositoryImpl implements KitDeMarcaRepository {

    private final IKitDeMarcaJpaRepository kitDeMarcaJpaRepository;

    private final KitDeMarcaRepositoryMapper kitDeMarcaRepositoryMapper;

    @Override
    public Optional<KitDeMarca> findByNegocioId(NegocioId negocioId) {
        return kitDeMarcaJpaRepository.findByNegocioId(negocioId.valor()).map(kitDeMarcaRepositoryMapper::toDomain);
    }

    @Override
    public KitDeMarca save(KitDeMarca kit) {
        return kitDeMarcaRepositoryMapper.toDomain(
                kitDeMarcaJpaRepository.save(kitDeMarcaRepositoryMapper.toEntity(kit)));
    }
}
