package com.ensambla.backend.infrastructure.presentation.dto.registro;

import com.ensambla.backend.domain.model.campo.CampoId;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Totales de todos los registros que cumplen la búsqueda, no sólo de la página: la cantidad y la suma de cada
 * campo numérico del bloque, indexada por id de campo.
 */
public record TotalesRegistrosResponse(long cantidad, Map<CampoId, BigDecimal> sumas) {
}
