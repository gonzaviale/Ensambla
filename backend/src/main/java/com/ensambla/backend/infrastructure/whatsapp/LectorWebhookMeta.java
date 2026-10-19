package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lee el aviso que manda Meta al webhook ({@code entry[].changes[].value.messages[]}) y se queda con los textos y las
 * notas de voz. Lo demás (avisos de entrega y lectura, fotos, stickers, ubicaciones) se ignora.
 */
@Component
@RequiredArgsConstructor
public class LectorWebhookMeta {

    private final ObjectMapper objectMapper;

    public RecibirMensajesWhatsappRequest leer(byte[] cuerpo) {
        List<MensajeEntrante> mensajes = new ArrayList<>();
        for (JsonNode entrada : arbol(cuerpo).path("entry")) {
            for (JsonNode cambio : entrada.path("changes")) {
                for (JsonNode mensaje : cambio.path("value").path("messages")) {
                    leerMensaje(mensaje).ifPresent(mensajes::add);
                }
            }
        }
        return new RecibirMensajesWhatsappRequest(mensajes);
    }

    /** Un cuerpo que no es JSON no trae mensajes. */
    private JsonNode arbol(byte[] cuerpo) {
        try {
            return objectMapper.readTree(cuerpo);
        } catch (JacksonException e) {
            return MissingNode.getInstance();
        }
    }

    private Optional<MensajeEntrante> leerMensaje(JsonNode mensaje) {
        String waMensajeId = mensaje.path("id").asString();
        String telefono = mensaje.path("from").asString();
        OffsetDateTime recibidoEn = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(Long.parseLong(mensaje.path("timestamp").asString())), ZoneOffset.UTC);
        return switch (mensaje.path("type").asString()) {
            case "text" -> Optional.of(new MensajeEntrante(
                    waMensajeId, telefono, "TEXTO", mensaje.path("text").path("body").asString(), null, recibidoEn));
            case "audio" -> Optional.of(new MensajeEntrante(
                    waMensajeId, telefono, "AUDIO", null, mensaje.path("audio").path("id").asString(), recibidoEn));
            default -> Optional.empty();
        };
    }
}
