package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CampoRepositoryImpl implements CampoRepository {

    private final ICampoJpaRepository jpa;
    private final CampoRepositoryMapper mapper;

    @Override
    public List<Campo> findByBloqueIdAndNegocioId(BloqueId bloqueId, NegocioId negocioId) {
        return jpa.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId.valor(), negocioId.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Campo> findByIdAndNegocioId(CampoId id, NegocioId negocioId) {
        return jpa.findByIdAndNegocioId(id.valor(), negocioId.valor()).map(mapper::toDomain);
    }

    @Override
    public List<Campo> findRelacionesHacia(BloqueId bloqueId, NegocioId negocioId) {
        return jpa.findRelacionesHacia(bloqueId.valor(), negocioId.valor()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Campo save(Campo campo) {
        return mapper.toDomain(jpa.save(mapper.toEntity(campo)));
    }

    @Override
    public void deleteByIdAndNegocioId(CampoId id, NegocioId negocioId) {
        jpa.deleteByIdAndNegocioId(id.valor(), negocioId.valor());
    }
}
