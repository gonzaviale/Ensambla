package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record IniciarPruebaRequest(UsuarioId usuarioId) {
}
