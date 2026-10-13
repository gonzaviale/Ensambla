package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;

import java.util.List;

/**
 * Las filas de un reporte y sus totales. Los totales son de todas las filas que cumplen el filtro; {@code filas}
 * trae las primeras (las más nuevas) hasta un máximo y {@code truncado} avisa que hay más.
 */
public record ReporteRegistros(List<Registro> filas, TotalesRegistros totales, boolean truncado) {
}
