package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La receta de una métrica; sin reparto elige no repartir y sin período mira el mes. */
public record MetricaWebRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull FuenteMetrica fuente,
        @NotNull CalculoMetrica calculo,
        AgrupacionMetrica agrupar,
        PeriodoMetrica periodo,
        Boolean soloFiado
) {

    /** Sin {@code soloFiado}, la receta mira todas las ventas. */
    public MetricaWebRequest {
        soloFiado = Boolean.TRUE.equals(soloFiado);
    }
}
