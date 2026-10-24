package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.model.receta.LineaReceta;

import java.util.List;
import java.util.UUID;

/** Reemplaza la receta del producto (un registro); sin líneas, la quita. */
public record GuardarRecetaRequest(
        UUID usuarioId,
        UUID registroId,
        UUID campoPrecioId,
        List<LineaRecetaRequest> lineas) {

    public List<LineaReceta> aLineasReceta() {
        return lineas.stream()
                .map(linea -> new LineaReceta(linea.insumoId(), linea.cantidad(), linea.unidad()))
                .toList();
    }
}
