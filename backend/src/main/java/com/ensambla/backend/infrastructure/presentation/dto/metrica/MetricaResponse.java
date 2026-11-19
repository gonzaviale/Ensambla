package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;

import java.util.List;

public record MetricaResponse(
        MetricaId id, DefinicionResponse definicion, int orden, AutoriaMetrica autoria, ResultadoResponse resultado,
        GraficoMetrica grafico, List<GraficoMetrica> graficosPosibles) {
}
