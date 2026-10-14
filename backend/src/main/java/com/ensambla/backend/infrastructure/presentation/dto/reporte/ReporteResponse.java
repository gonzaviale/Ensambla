package com.ensambla.backend.infrastructure.presentation.dto.reporte;

import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.TotalesRegistrosResponse;

import java.util.List;

/**
 * Las primeras filas de un reporte (las más nuevas) y los totales de todas las que cumplen el filtro: la cantidad y la
 * suma de cada columna numérica. {@code truncado} avisa que hay más filas que las {@code maximoFilas} que se muestran.
 */
public record ReporteResponse(
        List<RegistroResponse> filas,
        TotalesRegistrosResponse totales,
        boolean truncado,
        int maximoFilas
) {
}
