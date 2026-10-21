package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.dto.preferencia.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.dto.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.mapper.preferencia.PreferenciasItemMapper;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GuardarPreferenciasUseCase {

    private final PreferenciaService preferenciaService;

    private final PreferenciasItemMapper preferenciasItemMapper;

    private final Clock clock;

    public PreferenciasItem execute(GuardarPreferenciasRequest request) {
        return preferenciasItemMapper.toResponse(preferenciaService.guardar(
                request.usuarioId(),
                request.descartadas(),
                request.grupos(),
                request.bienvenidaVista(),
                request.nombre(),
                OffsetDateTime.now(clock)));
    }
}
