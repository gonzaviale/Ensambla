package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.useCase.plan.VencerPruebasUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.planes.pruebas", name = "habilitado",
        havingValue = "true", matchIfMissing = true)
public class VencimientoPruebasWorker {

    private final VencerPruebasUseCase vencerPruebasUseCase;

    @Scheduled(fixedDelayString = "${ensambla.planes.pruebas.intervalo:60000}",
            initialDelayString = "${ensambla.planes.pruebas.demora-inicial:10000}")
    public void vencer() {
        vencerPruebasUseCase.execute();
    }
}
