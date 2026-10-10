package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.ObtenerLoteRequest;
import com.ensambla.backend.domain.mapper.lote.ObtenerLoteMapper;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerLoteUseCase {

    private final NegocioService negocioService;

    private final LoteSubidaService loteSubidaService;

    private final ObtenerLoteMapper obtenerLoteMapper;

    public LoteItem execute(ObtenerLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        LoteSubida lote = loteSubidaService.obtener(negocioId, request.loteId());
        return obtenerLoteMapper.toResponse(lote);
    }
}
