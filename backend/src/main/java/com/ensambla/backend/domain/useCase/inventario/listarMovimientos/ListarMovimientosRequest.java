package com.ensambla.backend.domain.useCase.inventario.listarMovimientos;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Últimos movimientos de los productos de un bloque; con {@code registroId} sólo los de ese producto. */
public record ListarMovimientosRequest(UsuarioId usuarioId, BloqueId bloqueId, RegistroId registroId) {
}
