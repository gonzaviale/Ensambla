package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/** Filas de un bloque para elegir en un campo RELACION: por texto libre o, si vienen {@code ids}, sólo esas. */
public record ListarReferenciasRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        String texto,
        List<RegistroId> ids) {
}
