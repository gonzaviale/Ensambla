package com.ensambla.backend.infrastructure.presentation.dto.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;

import java.time.OffsetDateTime;
import java.util.List;

public record CampoResponse(
        CampoId id,
        BloqueId bloqueId,
        String nombre,
        String tipo,
        boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        BloqueId bloqueRelacionadoId,
        String formula,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
