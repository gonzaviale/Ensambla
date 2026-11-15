package com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record AbandonarMiniObjetivoRequest(UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId) {
}
