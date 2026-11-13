package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;

/**
 * El valor de un mini-objetivo al cierre de una semana. Lo guarda el resumen para comparar con los cierres siguientes.
 */
public record CierreMini(MiniObjetivoId miniObjetivoId, BigDecimal valor) {
}
