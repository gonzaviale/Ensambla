package com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record GenerarCodigoVinculacionRequest(UsuarioId usuarioId) {
}
