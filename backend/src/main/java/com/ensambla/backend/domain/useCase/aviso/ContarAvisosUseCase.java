package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ContarAvisosUseCase {

    private final NegocioService negocioService;
    private final AvisoService avisoService;

    public ContadorAvisos execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        return avisoService.contar(negocioId);
    }
}
