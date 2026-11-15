package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRespuestaMiniObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RespuestaMiniObjetivoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RespuestaMiniObjetivoRepositoryImpl implements RespuestaMiniObjetivoRepository {

    private final IRespuestaMiniObjetivoJpaRepository respuestaMiniObjetivoJpaRepository;

    private final RespuestaMiniObjetivoRepositoryMapper respuestaMiniObjetivoRepositoryMapper;

    @Override
    public RespuestaMiniObjetivo save(RespuestaMiniObjetivo respuesta) {
        return respuestaMiniObjetivoRepositoryMapper.toDomain(respuestaMiniObjetivoJpaRepository.saveAndFlush(
                respuestaMiniObjetivoRepositoryMapper.toEntity(respuesta)));
    }

    @Override
    public List<RespuestaMiniObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId) {
        return respuestaMiniObjetivoJpaRepository
                .findAllByObjetivoIdAndNegocioId(objetivoId.valor(), negocioId.valor()).stream()
                .map(respuestaMiniObjetivoRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByMiniObjetivoId(MiniObjetivoId miniObjetivoId) {
        return respuestaMiniObjetivoJpaRepository.existsByMiniObjetivoId(miniObjetivoId.valor());
    }
}
