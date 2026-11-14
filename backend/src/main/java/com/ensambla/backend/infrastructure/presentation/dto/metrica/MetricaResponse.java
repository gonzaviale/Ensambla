package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;

public record MetricaResponse(
        MetricaId id, DefinicionResponse definicion, int orden, AutoriaMetrica autoria, ResultadoResponse resultado) {
}
