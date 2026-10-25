package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLote {

    private final NegocioRepository negocioRepository;

    private final LoteSubidaRepository loteSubidaRepository;

    public LoteItem execute(ObtenerLoteRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        LoteSubida lote = loteSubidaRepository.obtener(negocioId, request.loteId());
        return LoteItem.de(lote);
    }
}
