package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IDecisionObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.DecisionObjetivoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DecisionObjetivoRepositoryImpl implements DecisionObjetivoRepository {

    private final IDecisionObjetivoJpaRepository decisionObjetivoJpaRepository;

    private final DecisionObjetivoRepositoryMapper decisionObjetivoRepositoryMapper;

    @Override
    public DecisionObjetivo save(DecisionObjetivo decision) {
        return decisionObjetivoRepositoryMapper.toDomain(
                decisionObjetivoJpaRepository.save(decisionObjetivoRepositoryMapper.toEntity(decision)));
    }

    @Override
    public List<DecisionObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId) {
        return decisionObjetivoJpaRepository
                .findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoId.valor(), negocioId.valor())
                .stream()
                .map(decisionObjetivoRepositoryMapper::toDomain)
                .toList();
    }
}
