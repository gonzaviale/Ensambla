package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente.AtenderMensajeCliente;
import com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp.AtenderMensajeWhatsapp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

/**
 * Mira cada tanto las colas de mensajes de WhatsApp, la de los comercios y la de los clientes, y los atiende en
 * orden, de a uno. Una cola que falla no frena a la otra.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.whatsapp.atencion", name = "habilitada", havingValue = "true")
public class AtencionWhatsappWorker {

    /** Tope de mensajes por vuelta y por cola: si hay una ráfaga, el resto espera a la próxima. */
    static final int MAX_POR_VUELTA = 20;

    private final AtenderMensajeWhatsapp atenderMensajeWhatsapp;
    private final AtenderMensajeCliente atenderMensajeCliente;

    @Scheduled(fixedDelayString = "${ensambla.whatsapp.atencion.intervalo:1500ms}")
    public void atender() {
        vaciar("de los comercios", atenderMensajeWhatsapp::execute);
        vaciar("de los clientes", atenderMensajeCliente::execute);
    }

    private static void vaciar(String cola, BooleanSupplier atender) {
        try {
            int atendidos = 0;
            while (atendidos < MAX_POR_VUELTA && atender.getAsBoolean()) {
                atendidos++;
            }
        } catch (RuntimeException e) {
            log.warn("La atención de WhatsApp {} no pudo completar una vuelta; los mensajes pendientes siguen en cola",
                    cola, e);
        }
    }
}
