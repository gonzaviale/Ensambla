package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarDecisionesRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
}
