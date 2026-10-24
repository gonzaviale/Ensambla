package com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias;

import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerPreferencias {

    private final PreferenciaService preferenciaService;

    private final Clock clock;

    public PreferenciasItem execute(ObtenerPreferenciasRequest request) {
        return PreferenciasItem.de(
                preferenciaService.obtener(request.usuarioId(), OffsetDateTime.now(clock)));
    }
}
