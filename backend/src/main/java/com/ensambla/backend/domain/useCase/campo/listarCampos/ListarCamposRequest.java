package com.ensambla.backend.domain.useCase.campo.listarCampos;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarCamposRequest(UsuarioId usuarioId, BloqueId bloqueId) {
}
