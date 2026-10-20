package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Responde por WhatsApp a través de Evolution API ({@code POST /message/sendText/{instancia}}). */
@Slf4j
public class ClienteEvolutionApi implements MensajeriaWhatsappPort {

    private final RestClient restClient;
    private final String instancia;
    private final String numeroBot;

    public ClienteEvolutionApi(RestClient restClient, String instancia, String numeroBot) {
        this.restClient = restClient;
        this.instancia = instancia;
        this.numeroBot = numeroBot;
    }

    /** Si el envío falla no se propaga: el mensaje recibido ya quedó atendido y el proveedor no debe reintentar. */
    @Override
    public void enviarTexto(String telefono, String texto) {
        try {
            restClient.post()
                    .uri("/message/sendText/{instancia}", instancia)
                    .body(Map.of("number", telefono, "text", texto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo responder por WhatsApp al {}: {}", telefono, e.getMessage());
        }
    }

    @Override
    public String numeroBot() {
        return numeroBot;
    }
}
