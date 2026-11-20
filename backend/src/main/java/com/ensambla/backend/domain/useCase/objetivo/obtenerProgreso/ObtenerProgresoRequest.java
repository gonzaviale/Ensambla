package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerProgresoRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
}
