package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivoId;
import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProgresoObjetivoRepositoryMapper {

    public ProgresoObjetivo toDomain(ProgresoObjetivoEntity source) {
        return new ProgresoObjetivo(
                ProgresoObjetivoId.de(source.getId()),
                ObjetivoId.de(source.getObjetivoId()),
                Identificador.opcional(source.getMiniObjetivoId(), MiniObjetivoId::de),
                source.getDia(),
                source.getValor(),
                source.getMedidoEn());
    }
}
