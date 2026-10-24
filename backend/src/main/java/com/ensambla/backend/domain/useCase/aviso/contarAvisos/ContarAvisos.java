package com.ensambla.backend.domain.useCase.aviso.contarAvisos;

import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ContarAvisos {

    private final NegocioService negocioService;
    private final AvisoService avisoService;

    public ContadorAvisos execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        return avisoService.contar(negocioId);
    }
}
