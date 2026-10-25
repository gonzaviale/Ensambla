package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAgregadoZonaProductoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AgregadoZonaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgregadoZonaRepositoryImpl implements AgregadoZonaRepository {

    private final IAgregadoZonaProductoJpaRepository agregadoJpaRepository;
    private final INegocioJpaRepository negocioJpaRepository;
    private final AgregadoZonaRepositoryMapper agregadoZonaRepositoryMapper;

    @Override
    public List<AgregadoZonaProducto> findByZonaId(ZonaId zonaId) {
        return agregadoJpaRepository.findAllByZonaIdOrderByNombreAscIdAsc(Identificador.valorDe(zonaId)).stream()
                .map(agregadoZonaRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public long contarComerciosQueComparten(ZonaId zonaId) {
        return negocioJpaRepository.countByZonaIdAndComparteDatosZonaTrue(Identificador.valorDe(zonaId));
    }
}
