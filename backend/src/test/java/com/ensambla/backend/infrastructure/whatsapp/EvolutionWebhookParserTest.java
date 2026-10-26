package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionWebhookParserTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private final JsonMapper json = JsonMapper.builder().build();
    private final EvolutionWebhookParser parser = new EvolutionWebhookParser(Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("Lee un texto simple con el teléfono, el id y la hora del mensaje")
    void parsearShouldReadPlainText() {
        Optional<MensajeEntrante> result = parsear(evento("messages.upsert",
                key("5491122334455@s.whatsapp.net", false, "ID1"),
                "\"message\":{\"conversation\":\"gasté 5000 en harina\"},\"messageTimestamp\":1792411200"));

        assertThat(result).contains(new MensajeEntrante("5491122334455", "ID1", TipoMensajeWhatsapp.TEXTO,
                "gasté 5000 en harina", null, null, AHORA));
    }

    @Test
    @DisplayName("Lee un texto que viene como mensaje extendido y acepta el evento en mayúsculas")
    void parsearShouldReadExtendedTextAndUppercaseEvent() {
        Optional<MensajeEntrante> result = parsear(evento("MESSAGES_UPSERT",
                key("5491122334455@s.whatsapp.net", false, "ID2"),
                "\"message\":{\"extendedTextMessage\":{\"text\":\"vendí 3 panes\"}}"));

        assertThat(result).get().satisfies(mensaje -> {
            assertThat(mensaje.texto()).isEqualTo("vendí 3 panes");
            assertThat(mensaje.recibidoEn()).isEqualTo(AHORA);
        });
    }

    @Test
    @DisplayName("Lee una nota de voz con su tipo de audio y su duración")
    void parsearShouldReadVoiceNote() {
        Optional<MensajeEntrante> result = parsear(evento("messages.upsert",
                key("5491122334455@s.whatsapp.net", false, "ID3"),
                "\"message\":{\"audioMessage\":{\"mimetype\":\"audio/ogg; codecs=opus\",\"seconds\":7}}"));

        assertThat(result).contains(new MensajeEntrante("5491122334455", "ID3", TipoMensajeWhatsapp.AUDIO, null,
                "audio/ogg; codecs=opus", 7, AHORA));
    }

    @Test
    @DisplayName("Una nota de voz sin tipo ni duración usa audio/ogg y duración vacía")
    void parsearShouldDefaultVoiceNoteMimeAndDuration() {
        Optional<MensajeEntrante> result = parsear(evento("messages.upsert",
                key("5491122334455@s.whatsapp.net", false, "ID4"), "\"message\":{\"audioMessage\":{}}"));

        assertThat(result).get().satisfies(mensaje -> {
            assertThat(mensaje.mimeAudio()).isEqualTo("audio/ogg");
            assertThat(mensaje.duracionSeg()).isNull();
        });
    }

    @Test
    @DisplayName("Con el direccionamiento @lid toma el número real de senderPn")
    void parsearShouldUseSenderPnWhenChatIsLid() {
        Optional<MensajeEntrante> result = parsear(evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID5\","
                        + "\"senderPn\":\"5491122334455@s.whatsapp.net\"}",
                "\"message\":{\"conversation\":\"hola\"}"));

        assertThat(result).get().extracting(MensajeEntrante::telefono).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Con @lid y sin senderPn usa remoteJidAlt")
    void parsearShouldUseRemoteJidAltWhenSenderPnIsMissing() {
        Optional<MensajeEntrante> result = parsear(evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID6\","
                        + "\"remoteJidAlt\":\"5491122334455@s.whatsapp.net\"}",
                "\"message\":{\"conversation\":\"hola\"}"));

        assertThat(result).get().extracting(MensajeEntrante::telefono).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Con @lid y sin número real ignora el mensaje")
    void parsearShouldIgnoreLidWithoutRealNumber() {
        assertThat(parsear(evento("messages.upsert",
                "\"key\":{\"remoteJid\":\"99887766@lid\",\"fromMe\":false,\"id\":\"ID7\"}",
                "\"message\":{\"conversation\":\"hola\"}"))).isEmpty();
    }

    @Test
    @DisplayName("Ignora otros eventos, mis propios mensajes, grupos, estados y mensajes sin id")
    void parsearShouldIgnoreEverythingThatIsNotAContactsMessage() {
        String texto = "\"message\":{\"conversation\":\"hola\"}";

        assertThat(parsear(evento("connection.update", key("5491122334455@s.whatsapp.net", false, "A"), texto)))
                .isEmpty();
        assertThat(parsear(evento("messages.upsert", key("5491122334455@s.whatsapp.net", true, "B"), texto)))
                .isEmpty();
        assertThat(parsear(evento("messages.upsert", key("120363@g.us", false, "C"), texto))).isEmpty();
        assertThat(parsear(evento("messages.upsert", key("status@broadcast", false, "D"), texto))).isEmpty();
        assertThat(parsear(evento("messages.upsert", key("5491122334455@s.whatsapp.net", false, ""), texto)))
                .isEmpty();
    }

    @Test
    @DisplayName("Ignora un chat con un número inválido y los mensajes que no son texto ni audio")
    void parsearShouldIgnoreInvalidPhonesAndOtherMessageTypes() {
        assertThat(parsear(evento("messages.upsert", key("abc@s.whatsapp.net", false, "E"),
                "\"message\":{\"conversation\":\"hola\"}"))).isEmpty();
        assertThat(parsear(evento("messages.upsert", key("5491122334455@s.whatsapp.net", false, "F"),
                "\"message\":{\"imageMessage\":{}}"))).isEmpty();
        assertThat(parsear(json.readTree("{}"))).isEmpty();
    }

    private Optional<MensajeEntrante> parsear(JsonNode cuerpo) {
        return parser.parsear(cuerpo);
    }

    private JsonNode evento(String evento, String key, String resto) {
        return json.readTree("{\"event\":\"" + evento + "\",\"data\":{" + key + "," + resto + "}}");
    }

    private static String key(String jid, boolean propio, String id) {
        return "\"key\":{\"remoteJid\":\"" + jid + "\",\"fromMe\":" + propio + ",\"id\":\"" + id + "\"}";
    }
}
