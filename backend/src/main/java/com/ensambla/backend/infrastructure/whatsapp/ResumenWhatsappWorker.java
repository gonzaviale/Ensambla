package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.whatsapp.enviarResumenPorWhatsapp.EnviarResumenPorWhatsapp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mira cada tanto si toca mandar el resumen de la mañana y se lo manda a quienes lo pidieron. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.whatsapp.resumen", name = "habilitado", havingValue = "true")
public class ResumenWhatsappWorker {

    private final EnviarResumenPorWhatsapp enviarResumenPorWhatsapp;

    @Scheduled(fixedDelayString = "${ensambla.whatsapp.resumen.intervalo:600000}",
            initialDelayString = "${ensambla.whatsapp.resumen.demora-inicial:60000}")
    public void enviar() {
        try {
            int enviados = enviarResumenPorWhatsapp.execute();
            log.debug("Resúmenes de la mañana enviados en esta vuelta: {}", enviados);
        } catch (RuntimeException e) {
            log.warn("El envío del resumen de la mañana no pudo completar una vuelta; se reintenta en la próxima", e);
        }
    }
}
