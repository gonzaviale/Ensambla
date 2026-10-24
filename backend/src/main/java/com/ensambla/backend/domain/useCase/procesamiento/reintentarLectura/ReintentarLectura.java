package com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura;

import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ReintentarLectura {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;
    private final Clock clock;

    public void execute(UsuarioLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        procesamientoService.reintentar(negocioId, request.loteId(), OffsetDateTime.now(clock));
    }
}
