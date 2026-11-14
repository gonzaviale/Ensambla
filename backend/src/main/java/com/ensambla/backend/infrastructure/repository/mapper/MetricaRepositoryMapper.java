package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import org.springframework.stereotype.Component;

@Component
public class MetricaRepositoryMapper {

    public Metrica toDomain(MetricaEntity source) {
        return Metrica.builder()
                .id(Identificador.opcional(source.getId(), MetricaId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .proyectoId(Identificador.opcional(source.getProyectoId(), ProyectoId::de))
                .definicion(new DefinicionMetrica(
                        source.getNombre(),
                        source.getFuente(),
                        source.getCalculo(),
                        source.getAgrupar(),
                        source.getPeriodo(),
                        source.isSoloFiado()))
                .orden(source.getOrden())
                .autoria(source.getAutoria())
                .creadoEn(source.getCreadoEn())
                .build();
    }

    public MetricaEntity toEntity(Metrica source) {
        DefinicionMetrica definicion = source.getDefinicion();
        return MetricaEntity.builder()
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .proyectoId(Identificador.valorDe(source.getProyectoId()))
                .nombre(definicion.nombre())
                .fuente(definicion.fuente())
                .calculo(definicion.calculo())
                .agrupar(definicion.agrupar())
                .periodo(definicion.periodo())
                .soloFiado(definicion.soloFiado())
                .orden(source.getOrden())
                .autoria(source.getAutoria())
                .creadoEn(source.getCreadoEn())
                .build();
    }
}
