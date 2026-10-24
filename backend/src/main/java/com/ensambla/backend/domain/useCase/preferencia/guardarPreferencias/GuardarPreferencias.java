package com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias;

import com.ensambla.backend.domain.service.preferencia.PreferenciaService;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GuardarPreferencias {

    private final PreferenciaService preferenciaService;

    private final Clock clock;

    public PreferenciasItem execute(GuardarPreferenciasRequest request) {
        return PreferenciasItem.de(preferenciaService.guardar(
                request.usuarioId(),
                request.descartadas(),
                request.grupos(),
                request.bienvenidaVista(),
                request.nombre(),
                OffsetDateTime.now(clock)));
    }
}
