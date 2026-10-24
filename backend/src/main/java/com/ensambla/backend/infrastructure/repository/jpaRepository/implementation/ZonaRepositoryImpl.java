package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IZonaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ZonaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ZonaRepositoryImpl implements ZonaRepository {

    private final IZonaJpaRepository zonaJpaRepository;
    private final ZonaRepositoryMapper zonaRepositoryMapper;

    @Override
    public List<Zona> findAll() {
        return zonaJpaRepository.findAll().stream().map(zonaRepositoryMapper::toDomain).toList();
    }

    @Override
    public Optional<Zona> findById(ZonaId id) {
        return zonaJpaRepository.findById(id.valor()).map(zonaRepositoryMapper::toDomain);
    }
}
