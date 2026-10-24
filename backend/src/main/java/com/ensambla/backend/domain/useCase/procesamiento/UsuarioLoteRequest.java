package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record UsuarioLoteRequest(UsuarioId usuarioId, LoteId loteId) {
}
