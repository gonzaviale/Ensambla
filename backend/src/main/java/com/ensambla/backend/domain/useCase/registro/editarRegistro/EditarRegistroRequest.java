package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Map;

public record EditarRegistroRequest(UsuarioId usuarioId, RegistroId registroId, Map<String, Object> valores) {
}
