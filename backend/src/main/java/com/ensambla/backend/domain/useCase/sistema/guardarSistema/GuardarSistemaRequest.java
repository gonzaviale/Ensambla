package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;

/** {@code id} es el de la URL y manda sobre el que traiga el cuerpo. */
public record GuardarSistemaRequest(UsuarioId usuarioId, String id, SistemaItem sistema) {
}
