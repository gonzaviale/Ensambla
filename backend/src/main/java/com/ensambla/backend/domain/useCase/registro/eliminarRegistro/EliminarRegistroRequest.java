package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarRegistroRequest(UsuarioId usuarioId, RegistroId registroId) {
}
