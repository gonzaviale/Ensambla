package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AvisoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AvisoRepositoryImpl implements AvisoRepository {

    private final IAvisoJpaRepository jpa;
    private final AvisoRepositoryMapper mapper;

    @Override
    public Aviso save(Aviso aviso) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(aviso)));
    }

    @Override
    public Optional<Aviso> findByIdAndNegocioId(UUID id, UUID negocioId) {
        return jpa.findByIdAndNegocioId(id, negocioId).map(mapper::toDomain);
    }

    @Override
    public Optional<Aviso> findByNegocioIdAndHuella(UUID negocioId, String huella) {
        return jpa.findByNegocioIdAndHuella(negocioId, huella).map(mapper::toDomain);
    }

    @Override
    public List<Aviso> findByNegocioIdAndTipoAndEstado(UUID negocioId, TipoAviso tipo, EstadoAviso estado) {
        return jpa.findByNegocioIdAndTipoAndEstado(negocioId, tipo, estado).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Aviso> findByNegocioId(UUID negocioId) {
        return jpa.findByNegocioId(negocioId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Aviso> findByNegocioIdAndEstado(UUID negocioId, EstadoAviso estado) {
        return jpa.findByNegocioIdAndEstado(negocioId, estado).stream().map(mapper::toDomain).toList();
    }
}
