package com.ensambla.backend.domain.useCase.metrica.listarMetricas;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarMetricasRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
