package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlantillaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlantillaRepositoryImpl implements PlantillaRepository {

    private final IPlantillaJpaRepository jpa;
    private final PlantillaRepositoryMapper mapper;

    @Override
    public List<Plantilla> findAllActivas() {
        return jpa.findByActivaTrueOrderByNombreAscIdAsc().stream().map(mapper::toDomain).toList();
    }
}
