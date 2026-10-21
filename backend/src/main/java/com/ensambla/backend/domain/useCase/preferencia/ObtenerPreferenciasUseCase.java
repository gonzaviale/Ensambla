package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.dto.preferencia.ObtenerPreferenciasRequest;
import com.ensambla.backend.domain.dto.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.mapper.preferencia.PreferenciasItemMapper;
import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerPreferenciasUseCase {

    private final PreferenciaService preferenciaService;

    private final PreferenciasItemMapper preferenciasItemMapper;

    private final Clock clock;

    public PreferenciasItem execute(ObtenerPreferenciasRequest request) {
        return preferenciasItemMapper.toResponse(
                preferenciaService.obtener(request.usuarioId(), OffsetDateTime.now(clock)));
    }
}
