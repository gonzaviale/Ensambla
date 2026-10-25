package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record RenombrarBloqueRequest(UsuarioId usuarioId, BloqueId bloqueId, String nombre) {
}
