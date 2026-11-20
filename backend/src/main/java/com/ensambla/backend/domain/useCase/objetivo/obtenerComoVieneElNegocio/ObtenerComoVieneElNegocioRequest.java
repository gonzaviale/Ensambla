package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de cómo viene el negocio del usuario: el negocio sale de la sesión. */
public record ObtenerComoVieneElNegocioRequest(UsuarioId usuarioId) {
}
