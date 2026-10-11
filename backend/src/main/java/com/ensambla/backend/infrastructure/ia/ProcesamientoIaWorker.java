package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.useCase.procesamiento.ProcesarLoteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.ia.procesamiento", name = "habilitado", havingValue = "true")
public class ProcesamientoIaWorker {

    private final ProcesarLoteUseCase procesarLoteUseCase;

    @Scheduled(fixedDelayString = "${ensambla.ia.procesamiento.intervalo:2000}")
    public void procesar() {
        try {
            procesarLoteUseCase.execute();
        } catch (RuntimeException e) {
            log.warn("El worker de IA no pudo completar un ciclo; recuperará el reclamo al vencer");
        }
    }
}
