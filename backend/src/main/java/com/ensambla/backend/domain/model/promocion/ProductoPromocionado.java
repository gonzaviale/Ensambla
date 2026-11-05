package com.ensambla.backend.domain.model.promocion;

import java.math.BigDecimal;

/** Un producto que se promociona, con el precio al que se vende. */
public record ProductoPromocionado(String nombre, BigDecimal precio) {
}
