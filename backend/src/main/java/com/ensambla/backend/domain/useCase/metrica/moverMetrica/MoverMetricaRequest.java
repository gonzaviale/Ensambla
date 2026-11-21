package com.ensambla.backend.domain.useCase.metrica.moverMetrica;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** @param lugar el lugar nuevo en el panel, empezando por el 0 */
public record MoverMetricaRequest(UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId, int lugar) {
}
