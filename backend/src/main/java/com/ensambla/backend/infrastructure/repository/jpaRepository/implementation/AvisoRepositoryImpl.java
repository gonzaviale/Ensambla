package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AvisoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
    public Optional<Aviso> findByIdAndNegocioId(AvisoId id, NegocioId negocioId) {
        return jpa.findByIdAndNegocioId(id.valor(), negocioId.valor()).map(mapper::toDomain);
    }

    @Override
    public Optional<Aviso> findByNegocioIdAndHuella(NegocioId negocioId, String huella) {
        return jpa.findByNegocioIdAndHuella(negocioId.valor(), huella).map(mapper::toDomain);
    }

    @Override
    public List<Aviso> findByNegocioIdAndTipoAndEstado(NegocioId negocioId, TipoAviso tipo, EstadoAviso estado) {
        return jpa.findByNegocioIdAndTipoAndEstado(negocioId.valor(), tipo, estado).stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Aviso> findByNegocioId(NegocioId negocioId) {
        return jpa.findByNegocioId(negocioId.valor()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Aviso> findByNegocioIdAndEstado(NegocioId negocioId, EstadoAviso estado) {
        return jpa.findByNegocioIdAndEstado(negocioId.valor(), estado).stream().map(mapper::toDomain).toList();
    }
}
