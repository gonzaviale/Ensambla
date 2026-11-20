package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMetricaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MetricaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MetricaRepositoryImpl implements MetricaRepository {

    private final IMetricaJpaRepository metricaJpaRepository;

    private final MetricaRepositoryMapper metricaRepositoryMapper;

    @Override
    public List<Metrica> findAllByProyectoIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId) {
        return metricaJpaRepository
                .findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(proyectoId.valor(), negocioId.valor()).stream()
                .map(metricaRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Metrica> findByIdAndProyectoIdAndNegocioId(
            MetricaId metricaId, ProyectoId proyectoId, NegocioId negocioId) {
        return metricaJpaRepository
                .findByIdAndProyectoIdAndNegocioId(metricaId.valor(), proyectoId.valor(), negocioId.valor())
                .map(metricaRepositoryMapper::toDomain);
    }

    @Override
    public Metrica save(Metrica metrica) {
        return metricaRepositoryMapper.toDomain(
                metricaJpaRepository.save(metricaRepositoryMapper.toEntity(metrica)));
    }

    @Override
    public void deleteByIdAndNegocioId(MetricaId metricaId, NegocioId negocioId) {
        metricaJpaRepository.deleteByIdAndNegocioId(metricaId.valor(), negocioId.valor());
    }
}
