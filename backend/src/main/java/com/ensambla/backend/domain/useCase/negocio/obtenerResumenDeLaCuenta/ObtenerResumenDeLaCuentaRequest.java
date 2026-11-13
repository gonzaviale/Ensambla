package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerResumenDeLaCuentaRequest(UsuarioId usuarioId) {
}
