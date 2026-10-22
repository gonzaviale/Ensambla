package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class VencerPruebasUseCase {

    private final PruebaPremiumService pruebaPremiumService;

    private final Clock clock;

    public void execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        List<UUID> negocios = pruebaPremiumService.listarVencidas(ahora);
        negocios.forEach(negocioId -> pruebaPremiumService.vencer(negocioId, ahora));
    }
}
