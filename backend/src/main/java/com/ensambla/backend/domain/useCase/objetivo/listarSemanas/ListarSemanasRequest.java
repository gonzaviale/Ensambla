package com.ensambla.backend.domain.useCase.objetivo.listarSemanas;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarSemanasRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
}
