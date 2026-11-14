package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import org.springframework.stereotype.Component;

@Component
public class RespuestaMiniObjetivoRepositoryMapper {

    public RespuestaMiniObjetivo toDomain(RespuestaMiniObjetivoEntity source) {
        return new RespuestaMiniObjetivo(
                Identificador.opcional(source.getId(), RespuestaMiniObjetivoId::de),
                Identificador.opcional(source.getNegocioId(), NegocioId::de),
                Identificador.opcional(source.getObjetivoId(), ObjetivoId::de),
                Identificador.opcional(source.getMiniObjetivoId(), MiniObjetivoId::de),
                source.getHizo(),
                source.getQuePaso(),
                source.getComentario(),
                source.getSemanaInicio(),
                source.getRespondidaEn());
    }

    public RespuestaMiniObjetivoEntity toEntity(RespuestaMiniObjetivo source) {
        return RespuestaMiniObjetivoEntity.builder()
                .id(Identificador.valorDe(source.id()))
                .negocioId(Identificador.valorDe(source.negocioId()))
                .objetivoId(Identificador.valorDe(source.objetivoId()))
                .miniObjetivoId(Identificador.valorDe(source.miniObjetivoId()))
                .hizo(source.hizo())
                .quePaso(source.quePaso())
                .comentario(source.comentario())
                .semanaInicio(source.semanaInicio())
                .respondidaEn(source.respondidaEn())
                .build();
    }
}
