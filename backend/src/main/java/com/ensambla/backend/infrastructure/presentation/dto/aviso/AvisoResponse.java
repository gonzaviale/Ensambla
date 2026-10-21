package com.ensambla.backend.infrastructure.presentation.dto.aviso;

import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.GrupoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AvisoResponse(
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
        OffsetDateTime resueltoEn
) {
}
