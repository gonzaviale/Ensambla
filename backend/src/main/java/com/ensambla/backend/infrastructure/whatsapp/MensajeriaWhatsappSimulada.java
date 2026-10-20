package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** No envía nada: deja las respuestas en el log y en memoria. Sirve para desarrollar y para los tests. */
@Slf4j
public class MensajeriaWhatsappSimulada implements MensajeriaWhatsappPort {

    public record Enviado(String telefono, String texto) {
    }

    private final List<Enviado> enviados = new CopyOnWriteArrayList<>();
    private final String numeroBot;

    public MensajeriaWhatsappSimulada(String numeroBot) {
        this.numeroBot = numeroBot;
    }

    @Override
    public void enviarTexto(String telefono, String texto) {
        log.info("WhatsApp simulado → {}: {}", telefono, texto);
        enviados.add(new Enviado(telefono, texto));
    }

    @Override
    public String numeroBot() {
        return numeroBot;
    }

    public List<Enviado> enviados() {
        return List.copyOf(enviados);
    }
}
