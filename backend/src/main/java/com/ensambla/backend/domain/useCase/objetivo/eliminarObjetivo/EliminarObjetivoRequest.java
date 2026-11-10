package com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarObjetivoRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
}
