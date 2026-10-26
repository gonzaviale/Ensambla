package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

/** Cuánto de un insumo (otra fila) lleva el producto; {@code unidad} es sólo una etiqueta ("kg", "lt"). */
public record LineaReceta(RegistroId insumoId, BigDecimal cantidad, String unidad) {
}
