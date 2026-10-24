package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Map;

public record CrearRegistroRequest(UsuarioId usuarioId, BloqueId bloqueId, Map<String, Object> valores) {
}
