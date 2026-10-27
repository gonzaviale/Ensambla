package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.Map;

/**
 * Habla con WhatsApp a través de Evolution API: responde con {@code POST /message/sendText/{instancia}} y baja las
 * fotos con {@code POST /chat/getBase64FromMediaMessage/{instancia}}.
 */
@Slf4j
public class ClienteEvolutionApi implements MensajeriaWhatsappPort {

    private static final String MIME_POR_DEFECTO = "image/jpeg";

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
    public boolean enviarTexto(String telefono, String texto) {
        try {
            restClient.post()
                    .uri("/message/sendText/{instancia}", instancia)
                    .body(Map.of("number", telefono, "text", texto))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("No se pudo responder por WhatsApp al {}: {}", telefono, e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArchivoWhatsapp descargarMedia(String idExterno) {
        Map<String, Object> cuerpo = restClient.post()
                .uri("/chat/getBase64FromMediaMessage/{instancia}", instancia)
                .body(Map.of("message", Map.of("key", Map.of("id", idExterno)), "convertToMp4", false))
                .retrieve()
                .body(Map.class);
        Object base64 = cuerpo == null ? null : cuerpo.get("base64");
        if (!(base64 instanceof String codificado) || codificado.isBlank()) {
            throw new IllegalStateException("Evolution no devolvió el archivo del mensaje " + idExterno);
        }
        Object mime = cuerpo.get("mimetype");
        return new ArchivoWhatsapp(Base64.getDecoder().decode(codificado),
                mime instanceof String texto && !texto.isBlank() ? texto : MIME_POR_DEFECTO);
    }

    @Override
    public String numeroBot() {
        return numeroBot;
    }
}
