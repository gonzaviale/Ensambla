package com.ensambla.backend.domain.useCase.metrica.cambiarGrafico;

import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CambiarGraficoRequest(
        UsuarioId usuarioId, ProyectoId proyectoId, MetricaId metricaId, GraficoMetrica grafico) {
}
