package com.ensambla.backend.domain.useCase.caja.obtenerCierre;

import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerCierreRequest(UsuarioId usuarioId, CierreCajaId id) {
}
