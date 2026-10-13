package com.ensambla.backend.infrastructure.presentation.dto.receta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecetaResponse(
        UUID bloqueInsumosId,
        UUID campoPrecioId,
        List<Linea> lineas,
        BigDecimal costo,
        boolean incompleta
) {

    public record Linea(
            UUID insumoId,
            String insumo,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
    }
}
