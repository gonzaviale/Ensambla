package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistroRepositoryMapper {

    public Registro toDomain(RegistroEntity entity) {
        return Registro.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .bloqueId(entity.getBloqueId())
                .valores(entity.getValores())
                .fotoOrigenId(entity.getFotoOrigenId())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public RegistroEntity toEntity(Registro registro) {
        return RegistroEntity.builder()
                .id(registro.getId())
                .negocioId(registro.getNegocioId())
                .bloqueId(registro.getBloqueId())
                .valores(registro.getValores())
                .fotoOrigenId(registro.getFotoOrigenId())
                .creadoEn(registro.getCreadoEn())
                .actualizadoEn(registro.getActualizadoEn())
                .build();
    }
}
