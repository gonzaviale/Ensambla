package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.DecisionObjetivoEntity;
import org.springframework.stereotype.Component;

@Component
public class DecisionObjetivoRepositoryMapper {

    public DecisionObjetivo toDomain(DecisionObjetivoEntity source) {
        return new DecisionObjetivo(
                DecisionObjetivoId.de(source.getId()),
                NegocioId.de(source.getNegocioId()),
                ObjetivoId.de(source.getObjetivoId()),
                Identificador.opcional(source.getMiniObjetivoId(), MiniObjetivoId::de),
                source.getTipo(),
                Identificador.opcional(source.getAvisoId(), AvisoId::de),
                Identificador.opcional(source.getRegistroId(), RegistroId::de),
                source.getDescripcion(),
                source.getTomadaEn(),
                source.getSemanaInicio());
    }

    public DecisionObjetivoEntity toEntity(DecisionObjetivo source) {
        return DecisionObjetivoEntity.builder()
                .id(source.id().valor())
                .negocioId(source.negocioId().valor())
                .objetivoId(source.objetivoId().valor())
                .miniObjetivoId(Identificador.valorDe(source.miniObjetivoId()))
                .tipo(source.tipo())
                .avisoId(Identificador.valorDe(source.avisoId()))
                .registroId(Identificador.valorDe(source.registroId()))
                .descripcion(source.descripcion())
                .tomadaEn(source.tomadaEn())
                .semanaInicio(source.semanaInicio())
                .build();
    }
}
