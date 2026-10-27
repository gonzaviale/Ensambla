package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Lee el evento {@code messages.upsert} de Evolution API. Devuelve vacío todo lo que no es un texto o una nota de voz
 * escrita por un contacto en un chat individual: mis propios mensajes, grupos, estados y otros eventos.
 */
public class EvolutionWebhookParser {

    private static final String EVENTO_MENSAJE = "messages.upsert";
    private static final String MIME_AUDIO_POR_DEFECTO = "audio/ogg";
    private static final String MIME_IMAGEN_POR_DEFECTO = "image/jpeg";
    private static final String SUFIJO_GRUPO = "@g.us";
    private static final String SUFIJO_LID = "@lid";
    private static final String ESTADOS = "status@broadcast";

    private final Clock clock;

    public EvolutionWebhookParser(Clock clock) {
        this.clock = clock;
    }

    public Optional<MensajeEntrante> parsear(JsonNode cuerpo) {
        if (!EVENTO_MENSAJE.equals(evento(cuerpo))) {
            return Optional.empty();
        }
        JsonNode data = cuerpo.path("data");
        JsonNode key = data.path("key");
        String idExterno = key.path("id").asString("");
        Optional<String> telefono = telefonoDelRemitente(key);
        if (key.path("fromMe").asBoolean(false) || idExterno.isBlank() || telefono.isEmpty()) {
            return Optional.empty();
        }
        OffsetDateTime recibidoEn = recibidoEn(data.path("messageTimestamp"));
        JsonNode mensaje = data.path("message");

        String texto = textoDe(mensaje);
        if (!texto.isBlank()) {
            return Optional.of(new MensajeEntrante(telefono.get(), idExterno, TipoMensajeWhatsapp.TEXTO, texto, null,
                    null, recibidoEn));
        }
        JsonNode audio = mensaje.path("audioMessage");
        if (audio.isObject()) {
            Integer duracion = audio.path("seconds").isNumber() ? audio.path("seconds").asInt() : null;
            return Optional.of(new MensajeEntrante(telefono.get(), idExterno, TipoMensajeWhatsapp.AUDIO, null,
                    audio.path("mimetype").asString(MIME_AUDIO_POR_DEFECTO), duracion, recibidoEn));
        }
        JsonNode imagen = mensaje.path("imageMessage");
        if (imagen.isObject()) {
            String pie = imagen.path("caption").asString("");
            return Optional.of(new MensajeEntrante(telefono.get(), idExterno, TipoMensajeWhatsapp.IMAGEN,
                    pie.isBlank() ? null : pie, imagen.path("mimetype").asString(MIME_IMAGEN_POR_DEFECTO), null,
                    recibidoEn));
        }
        return Optional.empty();
    }

    /** Evolution manda {@code messages.upsert} o {@code MESSAGES_UPSERT} según la versión y el transporte. */
    private static String evento(JsonNode cuerpo) {
        return cuerpo.path("event").asString("").toLowerCase().replace('_', '.');
    }

    private static String textoDe(JsonNode mensaje) {
        String conversacion = mensaje.path("conversation").asString("");
        return conversacion.isBlank() ? mensaje.path("extendedTextMessage").path("text").asString("") : conversacion;
    }

    /** Con el direccionamiento nuevo de WhatsApp el chat llega como {@code <id>@lid} y el número real va aparte. */
    private static Optional<String> telefonoDelRemitente(JsonNode key) {
        String jid = key.path("remoteJid").asString("");
        if (jid.endsWith(SUFIJO_GRUPO) || jid.equals(ESTADOS)) {
            return Optional.empty();
        }
        if (jid.endsWith(SUFIJO_LID)) {
            jid = key.path("senderPn").asString(key.path("remoteJidAlt").asString(""));
        }
        try {
            return Optional.of(TelefonoWhatsapp.de(jid).valor());
        } catch (BusinessRuleException e) {
            return Optional.empty();
        }
    }

    private OffsetDateTime recibidoEn(JsonNode timestamp) {
        Instant instante = timestamp.isNumber() ? Instant.ofEpochSecond(timestamp.asLong()) : clock.instant();
        return OffsetDateTime.ofInstant(instante, clock.getZone());
    }
}
