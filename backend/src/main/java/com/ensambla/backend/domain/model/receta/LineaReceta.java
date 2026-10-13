package com.ensambla.backend.domain.model.receta;

import java.math.BigDecimal;
import java.util.UUID;

/** Cuánto de un insumo (otra fila) lleva el producto; {@code unidad} es sólo una etiqueta ("kg", "lt"). */
public record LineaReceta(UUID insumoId, BigDecimal cantidad, String unidad) {
}
