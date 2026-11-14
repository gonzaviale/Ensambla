package com.ensambla.backend.domain.useCase.metrica.eliminarMetrica;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarMetricaRequest(UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId) {
}
