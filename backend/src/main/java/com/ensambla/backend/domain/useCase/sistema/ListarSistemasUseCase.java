package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.dto.sistema.ListarSistemasRequest;
import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.mapper.sistema.SistemaItemMapper;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarSistemasUseCase {

    private final NegocioService negocioService;

    private final SistemaService sistemaService;

    private final SistemaItemMapper sistemaItemMapper;

    public List<SistemaItem> execute(ListarSistemasRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        return sistemaItemMapper.toResponse(sistemaService.listar(negocioId));
    }
}
