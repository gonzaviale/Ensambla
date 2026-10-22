package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAgregadoZonaProductoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AgregadoZonaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgregadoZonaRepositoryImpl implements AgregadoZonaRepository {

    private final IAgregadoZonaProductoJpaRepository agregadoJpaRepository;
    private final INegocioJpaRepository negocioJpaRepository;
    private final AgregadoZonaRepositoryMapper agregadoZonaRepositoryMapper;

    @Override
    public List<AgregadoZonaProducto> findByZonaId(UUID zonaId) {
        return agregadoJpaRepository.findAllByZonaIdOrderByNombreAscIdAsc(zonaId).stream()
                .map(agregadoZonaRepositoryMapper::toDomain)
                .toList();
    }

    @Override
    public long contarComerciosQueComparten(UUID zonaId) {
        return negocioJpaRepository.countByZonaIdAndComparteDatosZonaTrue(zonaId);
    }
}
