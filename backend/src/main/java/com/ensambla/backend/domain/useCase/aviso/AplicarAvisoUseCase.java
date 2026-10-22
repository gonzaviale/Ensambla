package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ResolverAvisoRequest;
import com.ensambla.backend.domain.mapper.aviso.AvisoItemMapper;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarAvisoUseCase {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final AvisoItemMapper avisoItemMapper;
    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Aviso aviso = avisoService.obtener(request.avisoId(), negocioId);
        return avisoItemMapper.toResponse(avisoService.aplicar(aviso, OffsetDateTime.now(clock)));
    }
}
