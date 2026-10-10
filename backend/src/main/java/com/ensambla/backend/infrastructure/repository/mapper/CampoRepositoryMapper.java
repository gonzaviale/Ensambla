package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.springframework.stereotype.Component;

@Component
public class CampoRepositoryMapper {

    public Campo toDomain(CampoEntity entity) {
        return Campo.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .bloqueId(entity.getBloqueId())
                .nombre(entity.getNombre())
                .tipo(entity.getTipo())
                .obligatorio(entity.getObligatorio())
                .valorInicial(entity.getValorInicial())
                .opciones(entity.getOpciones())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public CampoEntity toEntity(Campo campo) {
        return CampoEntity.builder()
                .id(campo.getId())
                .negocioId(campo.getNegocioId())
                .bloqueId(campo.getBloqueId())
                .nombre(campo.getNombre())
                .tipo(campo.getTipo())
                .obligatorio(campo.getObligatorio())
                .valorInicial(campo.getValorInicial())
                .opciones(campo.getOpciones())
                .posicion(campo.getPosicion())
                .creadoEn(campo.getCreadoEn())
                .actualizadoEn(campo.getActualizadoEn())
                .build();
    }
}
