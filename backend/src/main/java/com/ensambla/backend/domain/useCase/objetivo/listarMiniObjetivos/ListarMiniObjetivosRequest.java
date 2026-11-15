package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarMiniObjetivosRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
}
