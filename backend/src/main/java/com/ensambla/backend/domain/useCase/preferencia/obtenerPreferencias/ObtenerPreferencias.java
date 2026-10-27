package com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias;

import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerPreferencias {

    private final PreferenciaRepository preferenciaRepository;

    private final Clock clock;

    public PreferenciasItem execute(ObtenerPreferenciasRequest request) {
        return PreferenciasItem.de(
                preferenciaRepository.obtenerOInicial(request.usuarioId(), OffsetDateTime.now(clock)));
    }
}
