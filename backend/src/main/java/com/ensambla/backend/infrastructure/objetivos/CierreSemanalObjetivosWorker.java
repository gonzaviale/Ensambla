package com.ensambla.backend.infrastructure.objetivos;

import com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos.CerrarSemanasObjetivos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cierra la semana de los objetivos los domingos a las 23:30 (hora argentina) y también al arrancar, para recuperar
 * las semanas en que el servidor estuvo caído. Cerrar dos veces la misma semana no duplica nada.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.objetivos.cierre", name = "habilitado", havingValue = "true")
public class CierreSemanalObjetivosWorker {

    private final CerrarSemanasObjetivos cerrarSemanasObjetivos;

    @Scheduled(cron = "${ensambla.objetivos.cierre.cron:0 30 23 * * SUN}", zone = "America/Argentina/Buenos_Aires")
    public void cerrar() {
        cerrarSemanasObjetivos.execute();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperar() {
        cerrarSemanasObjetivos.execute();
    }
}
