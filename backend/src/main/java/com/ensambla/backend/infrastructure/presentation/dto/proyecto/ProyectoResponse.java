package com.ensambla.backend.infrastructure.presentation.dto.proyecto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProyectoResponse(
        UUID id,
        String nombre,
        String descripcion,
        UUID plantillaId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        List<BloqueResponse> bloques
) {

    public record BloqueResponse(UUID id, String nombre, Short posicion, List<CampoResponse> campos) {
    }

    public record CampoResponse(
            UUID id,
            String nombre,
            String tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones,
            UUID bloqueRelacionadoId,
            Short posicion
    ) {
    }
}

