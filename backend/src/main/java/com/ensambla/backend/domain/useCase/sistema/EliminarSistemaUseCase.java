package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.dto.sistema.EliminarSistemaRequest;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarSistemaUseCase {

    private final NegocioService negocioService;

    private final SistemaService sistemaService;

    public void execute(EliminarSistemaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        sistemaService.eliminar(negocioId, IdSistema.aUuid(negocioId, request.id()));
    }
}
