package com.ensambla.backend.domain.dto.proyecto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProyectoItem(
        UUID id,
        String nombre,
        String descripcion,
        UUID plantillaId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        List<BloqueItem> bloques
) {

    public record BloqueItem(UUID id, String nombre, Short posicion, List<CampoItem> campos) {
    }

    public record CampoItem(
            UUID id,
            String nombre,
            String tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones,
            UUID bloqueRelacionadoId,
            String formula,
            Short posicion
    ) {
    }
}

