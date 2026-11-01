package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos.MedirImpactoAvisos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mide en forma periódica qué pasó con las ventas después de los avisos que la persona aplicó. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.avisos.impacto", name = "habilitado", havingValue = "true")
public class ImpactoAvisosWorker {

    private final MedirImpactoAvisos medirImpactoAvisos;

    @Scheduled(fixedDelayString = "${ensambla.avisos.impacto.intervalo:21600000}",
            initialDelayString = "${ensambla.avisos.impacto.demora-inicial:60000}")
    public void medir() {
        medirImpactoAvisos.execute();
    }
}
