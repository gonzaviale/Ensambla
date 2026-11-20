package com.ensambla.backend.domain.useCase.caja.cancelarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CancelarCobroQrRequest(UsuarioId usuarioId, CobroQrId id) {
}
