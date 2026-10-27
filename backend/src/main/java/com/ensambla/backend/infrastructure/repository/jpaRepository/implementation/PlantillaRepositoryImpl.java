package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlantillaRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PlantillaRepositoryImpl implements PlantillaRepository {

    private final IPlantillaJpaRepository jpa;
    private final PlantillaRepositoryMapper mapper;

    @Override
    public List<Plantilla> findAllActivas() {
        return jpa.findByActivaTrueOrderByNombreAscIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PlantillaProyecto> findAllParaProyectoActivas() {
        return jpa.findByActivaTrueOrderByNombreAscIdAsc().stream().map(this::toPlantillaProyecto).toList();
    }

    @Override
    public Optional<PlantillaProyecto> findParaProyectoByIdAndActivaTrue(PlantillaId plantillaId) {
        return jpa.findByIdAndActivaTrue(Identificador.valorDe(plantillaId)).map(this::toPlantillaProyecto);
    }

    private PlantillaProyecto toPlantillaProyecto(PlantillaEntity entity) {
        return new PlantillaProyecto(
                Identificador.opcional(entity.getId(), PlantillaId::de),
                entity.getNombre(),
                entity.getDescripcion(),
                JsonMapper.builder().build().convertValue(entity.getEstructura(), EstructuraPlantilla.class));
    }
}
