package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp.AtenderMensajeWhatsapp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mira cada tanto la cola de mensajes de WhatsApp y los atiende en orden, de a uno. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.whatsapp.atencion", name = "habilitada", havingValue = "true")
public class AtencionWhatsappWorker {

    /** Tope de mensajes por vuelta: si hay una ráfaga, el resto espera a la próxima. */
    static final int MAX_POR_VUELTA = 20;

    private final AtenderMensajeWhatsapp atenderMensajeWhatsapp;

    @Scheduled(fixedDelayString = "${ensambla.whatsapp.atencion.intervalo:1500ms}")
    public void atender() {
        try {
            int atendidos = 0;
            while (atendidos < MAX_POR_VUELTA && atenderMensajeWhatsapp.execute()) {
                atendidos++;
            }
        } catch (RuntimeException e) {
            log.warn("La atención de WhatsApp no pudo completar una vuelta; los mensajes pendientes siguen en cola", e);
        }
    }
}
