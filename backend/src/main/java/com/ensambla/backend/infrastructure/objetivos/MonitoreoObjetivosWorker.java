package com.ensambla.backend.infrastructure.objetivos;

import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mide en forma periódica y en silencio el avance de los objetivos y de sus mini-objetivos. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.objetivos.monitoreo", name = "habilitado", havingValue = "true")
public class MonitoreoObjetivosWorker {

    private final MonitorearObjetivos monitorearObjetivos;

    @Scheduled(fixedDelayString = "${ensambla.objetivos.monitoreo.intervalo:3600000}",
            initialDelayString = "${ensambla.objetivos.monitoreo.demora-inicial:90000}")
    public void monitorear() {
        monitorearObjetivos.execute();
    }
}
