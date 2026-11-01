package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerLoteRequest(UsuarioId usuarioId, LoteId loteId) {
}
