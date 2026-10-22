package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ListarAvisosRequest;
import com.ensambla.backend.domain.mapper.aviso.AvisoItemMapper;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarAvisosUseCase {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final AvisoItemMapper avisoItemMapper;

    public List<AvisoItem> execute(ListarAvisosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        return avisoItemMapper.toResponse(avisoService.listar(negocioId, request.estado()));
    }
}
