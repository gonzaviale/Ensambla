package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerRegistroRequest(UsuarioId usuarioId, RegistroId registroId) {
}
