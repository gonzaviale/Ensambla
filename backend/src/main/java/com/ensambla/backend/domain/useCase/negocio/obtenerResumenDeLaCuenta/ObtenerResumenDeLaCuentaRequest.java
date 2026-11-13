package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** {@code dias} son los últimos días a mirar, contando hoy; sin valor son 30. */
public record ObtenerResumenDeLaCuentaRequest(UsuarioId usuarioId, Integer dias) {
}
