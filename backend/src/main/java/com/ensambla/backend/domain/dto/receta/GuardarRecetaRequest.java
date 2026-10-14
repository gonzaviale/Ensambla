package com.ensambla.backend.domain.dto.receta;

import java.util.List;
import java.util.UUID;

/** Reemplaza la receta del producto (un registro); sin líneas, la quita. */
public record GuardarRecetaRequest(
        UUID usuarioId,
        UUID registroId,
        UUID campoPrecioId,
        List<LineaRecetaRequest> lineas) {
}
