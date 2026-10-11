package com.ensambla.backend.infrastructure.presentation.dto.registro;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Totales de todos los registros que cumplen la búsqueda, no sólo de la página: la cantidad y la suma de cada
 * campo numérico del bloque, indexada por id de campo.
 */
public record TotalesRegistrosResponse(long cantidad, Map<UUID, BigDecimal> sumas) {
}
