package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarCandidatosRequest(UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId) {
}
