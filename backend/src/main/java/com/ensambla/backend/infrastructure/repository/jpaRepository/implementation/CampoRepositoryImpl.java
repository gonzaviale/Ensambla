package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CampoRepositoryImpl implements CampoRepository {

    private final ICampoJpaRepository jpa;
    private final CampoRepositoryMapper mapper;

    @Override
    public List<Campo> findByBloqueIdAndNegocioId(UUID bloqueId, UUID negocioId) {
        return jpa.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Campo> findByIdAndNegocioId(UUID id, UUID negocioId) {
        return jpa.findByIdAndNegocioId(id, negocioId).map(mapper::toDomain);
    }

    @Override
    public List<Campo> findRelacionesHacia(UUID bloqueId, UUID negocioId) {
        return jpa.findRelacionesHacia(bloqueId, negocioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Campo save(Campo campo) {
        return mapper.toDomain(jpa.save(mapper.toEntity(campo)));
    }

    @Override
    public void deleteByIdAndNegocioId(UUID id, UUID negocioId) {
        jpa.deleteByIdAndNegocioId(id, negocioId);
    }
}
