package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import org.springframework.stereotype.Component;

@Component
public class ZonaRepositoryMapper {

    public Zona toDomain(ZonaEntity entity) {
        return Zona.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .radioMetros(entity.getRadioMetros())
                .build();
    }
}
