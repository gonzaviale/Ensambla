package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ObtenerFotoLoteRequest(UsuarioId usuarioId, LoteId loteId, FotoId fotoId) {
}
