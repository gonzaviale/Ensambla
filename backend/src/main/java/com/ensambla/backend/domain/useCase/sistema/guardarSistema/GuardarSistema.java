package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GuardarSistema {

    private final NegocioService negocioService;

    private final SistemaService sistemaService;

    private final Clock clock;

    public SistemaItem execute(GuardarSistemaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Sistema sistema = request.sistema().aSistema(negocioId, request.id(), OffsetDateTime.now(clock));
        return SistemaItem.de(sistemaService.guardar(negocioId, sistema));
    }
}
