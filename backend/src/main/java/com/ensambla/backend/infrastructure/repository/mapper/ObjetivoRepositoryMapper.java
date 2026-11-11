package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import org.springframework.stereotype.Component;

@Component
public class ObjetivoRepositoryMapper {

    public Objetivo toDomain(ObjetivoEntity source) {
        return Objetivo.builder()
                .id(Identificador.opcional(source.getId(), ObjetivoId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .texto(source.getTexto())
                .meta(source.getMeta())
                .creadoEn(source.getCreadoEn())
                .build();
    }

    public ObjetivoEntity toEntity(Objetivo source) {
        return ObjetivoEntity.builder()
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .texto(source.getTexto())
                .meta(source.getMeta())
                .creadoEn(source.getCreadoEn())
                .build();
    }
}
