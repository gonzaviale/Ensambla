package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.springframework.stereotype.Component;

@Component
public class CampoRepositoryMapper {

    public Campo toDomain(CampoEntity entity) {
        return Campo.builder()
                .id(Identificador.opcional(entity.getId(), CampoId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .bloqueId(Identificador.opcional(entity.getBloqueId(), BloqueId::de))
                .nombre(entity.getNombre())
                .tipo(entity.getTipo())
                .obligatorio(entity.getObligatorio())
                .valorInicial(entity.getValorInicial())
                .opciones(entity.getOpciones())
                .bloqueRelacionadoId(Identificador.opcional(entity.getBloqueRelacionadoId(), BloqueId::de))
                .formula(entity.getFormula())
                .clave(entity.getClave())
                .minimo(entity.getMinimo())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public CampoEntity toEntity(Campo campo) {
        return CampoEntity.builder()
                .id(Identificador.valorDe(campo.getId()))
                .negocioId(Identificador.valorDe(campo.getNegocioId()))
                .bloqueId(Identificador.valorDe(campo.getBloqueId()))
                .nombre(campo.getNombre())
                .tipo(campo.getTipo())
                .obligatorio(campo.getObligatorio())
                .valorInicial(campo.getValorInicial())
                .opciones(campo.getOpciones())
                .bloqueRelacionadoId(Identificador.valorDe(campo.getBloqueRelacionadoId()))
                .formula(campo.getFormula())
                .clave(campo.getClave())
                .minimo(campo.getMinimo())
                .posicion(campo.getPosicion())
                .creadoEn(campo.getCreadoEn())
                .actualizadoEn(campo.getActualizadoEn())
                .build();
    }
}
