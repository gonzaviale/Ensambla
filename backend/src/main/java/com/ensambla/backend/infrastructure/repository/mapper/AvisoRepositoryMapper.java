package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import org.springframework.stereotype.Component;

@Component
public class AvisoRepositoryMapper {

    public Aviso toDomain(AvisoEntity entity) {
        return Aviso.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .proyectoId(entity.getProyectoId())
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
                .id(aviso.getId())
                .negocioId(aviso.getNegocioId())
                .proyectoId(aviso.getProyectoId())
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
