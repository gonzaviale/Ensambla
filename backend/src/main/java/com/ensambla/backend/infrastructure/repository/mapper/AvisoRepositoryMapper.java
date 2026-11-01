package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import org.springframework.stereotype.Component;

@Component
public class AvisoRepositoryMapper {

    public Aviso toDomain(AvisoEntity entity) {
        return Aviso.builder()
                .id(Identificador.opcional(entity.getId(), AvisoId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .proyectoId(Identificador.opcional(entity.getProyectoId(), ProyectoId::de))
                .tipo(entity.getTipo())
                .grupo(entity.getGrupo())
                .prioridad(entity.getPrioridad())
                .entidad(entity.getEntidad())
                .huella(entity.getHuella())
                .datos(entity.getDatos())
                .titulo(entity.getTitulo())
                .detalle(entity.getDetalle())
                .motivos(entity.getMotivos())
                .acciones(entity.getAcciones())
                .estado(entity.getEstado())
                .resueltoEn(entity.getResueltoEn())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public AvisoEntity toEntity(Aviso aviso) {
        return AvisoEntity.builder()
                .id(Identificador.valorDe(aviso.getId()))
                .negocioId(Identificador.valorDe(aviso.getNegocioId()))
                .proyectoId(Identificador.valorDe(aviso.getProyectoId()))
                .tipo(aviso.getTipo())
                .grupo(aviso.getGrupo())
                .prioridad(aviso.getPrioridad())
                .entidad(aviso.getEntidad())
                .huella(aviso.getHuella())
                .datos(aviso.getDatos())
                .titulo(aviso.getTitulo())
                .detalle(aviso.getDetalle())
                .motivos(aviso.getMotivos())
                .acciones(aviso.getAcciones())
                .estado(aviso.getEstado())
                .resueltoEn(aviso.getResueltoEn())
                .creadoEn(aviso.getCreadoEn())
                .actualizadoEn(aviso.getActualizadoEn())
                .build();
    }
}
