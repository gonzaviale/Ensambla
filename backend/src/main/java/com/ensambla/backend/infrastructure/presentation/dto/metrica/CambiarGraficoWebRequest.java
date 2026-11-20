package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import jakarta.validation.constraints.NotNull;

/** Cómo se quiere ver la métrica: número, barras, dona o línea. */
public record CambiarGraficoWebRequest(@NotNull GraficoMetrica grafico) {
}
