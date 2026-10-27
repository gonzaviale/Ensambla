package com.ensambla.backend.domain.useCase.receta.obtenerReceta;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerRecetaRequest(UsuarioId usuarioId, RegistroId registroId) {
}
