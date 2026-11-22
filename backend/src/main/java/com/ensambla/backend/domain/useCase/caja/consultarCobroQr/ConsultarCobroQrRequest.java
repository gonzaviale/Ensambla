package com.ensambla.backend.domain.useCase.caja.consultarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ConsultarCobroQrRequest(UsuarioId usuarioId, CobroQrId id) {
}
