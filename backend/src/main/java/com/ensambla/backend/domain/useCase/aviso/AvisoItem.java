package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AvisoItem(
        UUID id,
        UUID proyectoId,
        TipoAviso tipo,
        GrupoAviso grupo,
        short prioridad,
        EstadoAviso estado,
        String entidad,
        String titulo,
        String detalle,
        List<Object> motivos,
        List<Object> acciones,
        Map<String, Object> datos,
        OffsetDateTime creadoEn,
        OffsetDateTime resueltoEn) {

    public static AvisoItem de(Aviso aviso) {
        return new AvisoItem(
                aviso.getId(),
                aviso.getProyectoId(),
                aviso.getTipo(),
                aviso.getGrupo(),
                aviso.getPrioridad(),
                aviso.getEstado(),
                aviso.getEntidad(),
                aviso.getTitulo(),
                aviso.getDetalle(),
                aviso.getMotivos(),
                aviso.getAcciones(),
                aviso.getDatos(),
                aviso.getCreadoEn(),
                aviso.getResueltoEn());
    }
}
