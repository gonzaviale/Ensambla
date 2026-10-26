package com.ensambla.backend.infrastructure.presentation.dto.proyecto;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;
import java.util.List;

public record ProyectoResponse(
        ProyectoId id,
        String nombre,
        String descripcion,
        PlantillaId plantillaId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        List<BloqueResponse> bloques
) {

    public record BloqueResponse(BloqueId id, String nombre, Short posicion, List<CampoResponse> campos) {
    }

    public record CampoResponse(
            CampoId id,
            String nombre,
            String tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones,
            BloqueId bloqueRelacionadoId,
            String formula,
            Short posicion
    ) {
    }
}

