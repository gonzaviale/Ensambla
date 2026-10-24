package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerLote {

    private final NegocioService negocioService;

    private final LoteSubidaService loteSubidaService;

    public LoteItem execute(ObtenerLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        LoteSubida lote = loteSubidaService.obtener(negocioId, request.loteId());
        return LoteItem.de(lote);
    }
}
