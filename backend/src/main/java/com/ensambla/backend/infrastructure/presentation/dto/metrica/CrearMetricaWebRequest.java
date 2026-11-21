package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La receta a guardar y si salió de las sugeridas del rubro o la armó la persona. */
public record CrearMetricaWebRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull FuenteMetrica fuente,
        @NotNull CalculoMetrica calculo,
        AgrupacionMetrica agrupar,
        PeriodoMetrica periodo,
        Boolean soloFiado,
        @NotNull AutoriaMetrica autoria
) {

    /** Sin {@code soloFiado}, la receta mira todas las ventas. */
    public CrearMetricaWebRequest {
        soloFiado = Boolean.TRUE.equals(soloFiado);
    }
}
