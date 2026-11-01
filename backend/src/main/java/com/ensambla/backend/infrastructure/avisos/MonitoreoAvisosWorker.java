package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el motor de monitoreo en forma periódica, sin que el usuario tenga que pedirlo. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.avisos.motor", name = "habilitado", havingValue = "true")
public class MonitoreoAvisosWorker {

    private final EjecutarMotorAvisos ejecutarMotorAvisos;

    @Scheduled(fixedDelayString = "${ensambla.avisos.motor.intervalo:300000}",
            initialDelayString = "${ensambla.avisos.motor.demora-inicial:30000}")
    public void monitorear() {
        ejecutarMotorAvisos.execute();
    }
}
