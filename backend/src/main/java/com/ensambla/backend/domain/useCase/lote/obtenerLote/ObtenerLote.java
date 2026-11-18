package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLote {

    private final NegocioRepository negocioRepository;

    private final LoteSubidaRepository loteSubidaRepository;

    public LoteItem execute(ObtenerLoteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        LoteSubida lote = obtenerLote(negocioId, request.loteId());
        return LoteItem.de(lote);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private LoteSubida obtenerLote(NegocioId negocioId, LoteId loteId) {
        return loteSubidaRepository.obtener(negocioId, loteId);
    }
}
