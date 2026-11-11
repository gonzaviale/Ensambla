package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MiniObjetivoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MiniObjetivoRepositoryImpl implements MiniObjetivoRepository {

    private final IMiniObjetivoJpaRepository miniObjetivoJpaRepository;

    private final MiniObjetivoRepositoryMapper miniObjetivoRepositoryMapper;

    @Override
    public List<MiniObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId) {
        return miniObjetivoJpaRepository
                .findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoId.valor(), negocioId.valor())
                .stream()
                .map(miniObjetivoRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MiniObjetivo> findByIdAndObjetivoIdAndNegocioId(
            MiniObjetivoId miniObjetivoId, ObjetivoId objetivoId, NegocioId negocioId) {
        return miniObjetivoJpaRepository
                .findByIdAndObjetivoIdAndNegocioId(miniObjetivoId.valor(), objetivoId.valor(), negocioId.valor())
                .map(miniObjetivoRepositoryMapper::toDomain);
    }

    @Override
    public MiniObjetivo save(MiniObjetivo miniObjetivo) {
        return miniObjetivoRepositoryMapper.toDomain(
                miniObjetivoJpaRepository.saveAndFlush(miniObjetivoRepositoryMapper.toEntity(miniObjetivo)));
    }
}
