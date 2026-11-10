package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ObjetivoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ObjetivoRepositoryImpl implements ObjetivoRepository {

    private final IObjetivoJpaRepository objetivoJpaRepository;

    private final ObjetivoRepositoryMapper objetivoRepositoryMapper;

    @Override
    public List<Objetivo> findAllByNegocioId(NegocioId negocioId) {
        return objetivoJpaRepository.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId.valor()).stream()
                .map(objetivoRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Objetivo> findByIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId) {
        return objetivoJpaRepository.findByIdAndNegocioId(objetivoId.valor(), negocioId.valor())
                .map(objetivoRepositoryMapper::toDomain);
    }

    @Override
    public Objetivo save(Objetivo objetivo) {
        return objetivoRepositoryMapper.toDomain(
                objetivoJpaRepository.save(objetivoRepositoryMapper.toEntity(objetivo)));
    }

    @Override
    public void deleteByIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId) {
        objetivoJpaRepository.deleteByIdAndNegocioId(objetivoId.valor(), negocioId.valor());
    }
}
