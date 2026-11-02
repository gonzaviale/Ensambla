package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IHistorialPrecioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.HistorialPrecioRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HistorialPrecioRepositoryImpl implements HistorialPrecioRepository {

    private final IHistorialPrecioJpaRepository jpa;
    private final HistorialPrecioRepositoryMapper mapper;

    @Override
    public CambioPrecio save(CambioPrecio cambio) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(cambio)));
    }

    @Override
    public List<CambioPrecio> findByNegocioIdDesde(NegocioId negocioId, OffsetDateTime desde) {
        return jpa.findByNegocioIdAndCambiadoEnGreaterThanEqualOrderByCambiadoEnAsc(negocioId.valor(), desde).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
