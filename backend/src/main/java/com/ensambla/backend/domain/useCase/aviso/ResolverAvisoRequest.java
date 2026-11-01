package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de aplicar o descartar un aviso del negocio del usuario. */
public record ResolverAvisoRequest(UsuarioId usuarioId, AvisoId avisoId) {
}
