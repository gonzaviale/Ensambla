package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import org.springframework.stereotype.Component;

@Component
public class PlantillaRepositoryMapper {

    public Plantilla toDomain(PlantillaEntity entity) {
        return Plantilla.builder()
                .id(Identificador.opcional(entity.getId(), PlantillaId::de))
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .icono(entity.getIcono())
                .estructura(entity.getEstructura())
                .activa(entity.getActiva())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }
}
