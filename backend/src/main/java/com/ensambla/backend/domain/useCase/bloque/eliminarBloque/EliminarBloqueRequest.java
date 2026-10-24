package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record EliminarBloqueRequest(UsuarioId usuarioId, BloqueId bloqueId) {
}
