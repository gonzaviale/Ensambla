package com.ensambla.backend.domain.useCase.plan.desbloquearContenidos;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

public record DesbloquearContenidosRequest(UsuarioId usuarioId, String tipo, List<String> claves) {
}
