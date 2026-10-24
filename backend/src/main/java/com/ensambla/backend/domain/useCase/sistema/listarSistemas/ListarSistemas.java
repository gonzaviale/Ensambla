package com.ensambla.backend.domain.useCase.sistema.listarSistemas;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarSistemas {

    private final NegocioService negocioService;

    private final SistemaService sistemaService;

    public List<SistemaItem> execute(ListarSistemasRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        return sistemaService.listar(negocioId).stream().map(SistemaItem::de).toList();
    }
}
