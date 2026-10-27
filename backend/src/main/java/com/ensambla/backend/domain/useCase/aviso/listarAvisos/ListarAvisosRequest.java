package com.ensambla.backend.domain.useCase.aviso.listarAvisos;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** {@code estado} es opcional: sin estado se listan todos los avisos del negocio. */
public record ListarAvisosRequest(UsuarioId usuarioId, EstadoAviso estado) {
}
