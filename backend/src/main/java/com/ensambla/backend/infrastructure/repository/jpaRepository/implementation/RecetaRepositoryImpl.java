package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRecetaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RecetaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecetaRepositoryImpl implements RecetaRepository {

    private final IRecetaJpaRepository jpa;

    private final RecetaRepositoryMapper mapper;

    @Override
    public Receta save(Receta receta) {
        return mapper.toDomain(jpa.save(mapper.toEntity(receta)));
    }

    @Override
    public Optional<Receta> findByProductoIdAndNegocioId(RegistroId productoId, NegocioId negocioId) {
        return jpa.findByProductoIdAndNegocioId(productoId.valor(), negocioId.valor()).map(mapper::toDomain);
    }

    @Override
    public void deleteByProductoIdAndNegocioId(RegistroId productoId, NegocioId negocioId) {
        jpa.deleteByProductoIdAndNegocioId(productoId.valor(), negocioId.valor());
    }
}
