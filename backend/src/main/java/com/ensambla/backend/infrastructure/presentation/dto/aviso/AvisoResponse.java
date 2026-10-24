package com.ensambla.backend.infrastructure.presentation.dto.aviso;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AvisoResponse(
        AvisoId id,
        ProyectoId proyectoId,
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
        OffsetDateTime resueltoEn
) {
}
