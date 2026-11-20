package com.ensambla.backend.domain.useCase.caja.generarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;

public record GenerarCobroQrRequest(UsuarioId usuarioId, CobroQrId id, BigDecimal importe) {
}
