package com.ensambla.backend.infrastructure.presentation.mapper.receta;

import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.receta.RecetaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.receta.RecetaResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RecetaPresentationMapper {

    public ObtenerRecetaRequest toObtenerRequest(UUID usuarioId, UUID registroId) {
        return new ObtenerRecetaRequest(usuarioId, registroId);
    }

    /** Sin {@code lineas} (clave ausente) se quita la receta. */
    public GuardarRecetaRequest toGuardarRequest(UUID usuarioId, UUID registroId, RecetaRequest request) {
        List<RecetaRequest.Linea> lineas = request.lineas() == null ? List.of() : request.lineas();
        return new GuardarRecetaRequest(
                usuarioId,
                registroId,
                request.campoPrecioId(),
                lineas.stream()
                        .map(linea -> new LineaRecetaRequest(linea.insumoId(), linea.cantidad(), linea.unidad()))
                        .toList());
    }

    public RecetaResponse toResponse(RecetaItem receta) {
        return new RecetaResponse(
                receta.bloqueInsumosId(),
                receta.campoPrecioId(),
                receta.lineas().stream()
                        .map(linea -> new RecetaResponse.Linea(
                                linea.insumoId(),
                                linea.insumo(),
                                linea.cantidad(),
                                linea.unidad(),
                                linea.precioUnitario(),
                                linea.subtotal()))
                        .toList(),
                receta.costo(),
                receta.incompleta());
    }
}
