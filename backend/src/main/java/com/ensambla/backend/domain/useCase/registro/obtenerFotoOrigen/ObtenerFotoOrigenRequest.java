package com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerFotoOrigenRequest(UsuarioId usuarioId, RegistroId registroId) {
}
