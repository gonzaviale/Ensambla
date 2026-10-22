package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.dto.sistema.GuardarSistemaRequest;
import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.mapper.sistema.SistemaItemMapper;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.sistema.SistemaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GuardarSistemaUseCase {

    private final NegocioService negocioService;

    private final SistemaService sistemaService;

    private final SistemaItemMapper sistemaItemMapper;

    private final Clock clock;

    public SistemaItem execute(GuardarSistemaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Sistema sistema = sistemaItemMapper.toSistema(
                negocioId, request.id(), request.sistema(), OffsetDateTime.now(clock));
        return sistemaItemMapper.toResponse(sistemaService.guardar(negocioId, sistema));
    }
}
