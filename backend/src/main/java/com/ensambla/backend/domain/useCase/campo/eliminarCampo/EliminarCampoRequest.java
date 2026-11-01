package com.ensambla.backend.domain.useCase.campo.eliminarCampo;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarCampoRequest(UsuarioId usuarioId, CampoId campoId) {
}
