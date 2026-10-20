package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.UUID;

/**
 * La propuesta final de Sami con los ajustes de la persona.
 *
 * @param nombre   opcional; sin él, el proyecto se llama como la plantilla
 * @param columnas las columnas extra que la persona dejó
 */
public record CrearProyectoDesdeSamiRequest(
        UUID usuarioId,
        UUID plantillaId,
        String nombre,
        List<ColumnaSamiItem> columnas
) {
}
