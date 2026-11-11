package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import org.springframework.stereotype.Component;

@Component
public class MiniObjetivoRepositoryMapper {

    public MiniObjetivo toDomain(MiniObjetivoEntity source) {
        return MiniObjetivo.builder()
                .id(Identificador.opcional(source.getId(), MiniObjetivoId::de))
                .objetivoId(Identificador.opcional(source.getObjetivoId(), ObjetivoId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .posicion(source.getPosicion())
                .tipo(source.getTipo())
                .registroId(Identificador.opcional(source.getRegistroId(), RegistroId::de))
                .referencia(source.getReferencia())
                .meta(source.getMeta())
                .valorInicial(source.getValorInicial())
                .valorActual(source.getValorActual())
                .aporteEstimado(source.getAporteEstimado())
                .estado(source.getEstado())
                .semanaInicio(source.getSemanaInicio())
                .titulo(source.getTitulo())
                .justificacion(source.getJustificacion())
                .textoConIa(source.isTextoConIa())
                .reemplazadoPor(Identificador.opcional(source.getReemplazadoPor(), MiniObjetivoId::de))
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public MiniObjetivoEntity toEntity(MiniObjetivo source) {
        return MiniObjetivoEntity.builder()
                .id(Identificador.valorDe(source.getId()))
                .objetivoId(Identificador.valorDe(source.getObjetivoId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .posicion((short) source.getPosicion())
                .tipo(source.getTipo())
                .registroId(Identificador.valorDe(source.getRegistroId()))
                .referencia(source.getReferencia())
                .meta(source.getMeta())
                .valorInicial(source.getValorInicial())
                .valorActual(source.getValorActual())
                .aporteEstimado(source.getAporteEstimado())
                .estado(source.getEstado())
                .semanaInicio(source.getSemanaInicio())
                .titulo(source.getTitulo())
                .justificacion(source.getJustificacion())
                .textoConIa(source.isTextoConIa())
                .reemplazadoPor(Identificador.valorDe(source.getReemplazadoPor()))
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }
}
