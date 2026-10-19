package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest;
import com.ensambla.backend.domain.dto.whatsapp.RecibirMensajesWhatsappRequest.MensajeEntrante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_AUDIO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_ESTADO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_TEXTO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.aviso;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.bytes;
import static org.assertj.core.api.Assertions.assertThat;

class LectorWebhookMetaTest {

    private static final OffsetDateTime RECIBIDO_EN = OffsetDateTime.parse("2026-10-19T11:59:30Z");

    private final LectorWebhookMeta lectorWebhookMeta = new LectorWebhookMeta(JsonMapper.builder().build());

    @Test
    @DisplayName("Lee un texto con su número, su id y la hora en que llegó")
    void leerShouldReadATextMessage() {
        RecibirMensajesWhatsappRequest result = lectorWebhookMeta.leer(bytes(AVISO_TEXTO));

        assertThat(result.mensajes()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.waMensajeId()).isEqualTo("wamid.texto");
            assertThat(mensaje.telefono()).isEqualTo("5491122334455");
            assertThat(mensaje.tipo()).isEqualTo("TEXTO");
            assertThat(mensaje.texto()).isEqualTo("Vendí 3 velas a Marta a 4500");
            assertThat(mensaje.mediaId()).isNull();
            assertThat(mensaje.recibidoEn()).isEqualTo(RECIBIDO_EN);
        });
    }

    @Test
    @DisplayName("Lee una nota de voz con el id del audio")
    void leerShouldReadAnAudioMessage() {
        RecibirMensajesWhatsappRequest result = lectorWebhookMeta.leer(bytes(AVISO_AUDIO));

        assertThat(result.mensajes()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.waMensajeId()).isEqualTo("wamid.audio");
            assertThat(mensaje.tipo()).isEqualTo("AUDIO");
            assertThat(mensaje.mediaId()).isEqualTo("1234567890123456");
            assertThat(mensaje.texto()).isNull();
        });
    }

    @Test
    @DisplayName("Ignora fotos, stickers y demás tipos, pero lee los textos del mismo aviso")
    void leerShouldSkipUnsupportedTypes() {
        String foto = "{\"from\":\"1\",\"id\":\"wamid.foto\",\"timestamp\":\"1792411170\",\"type\":\"image\","
                + "\"image\":{\"id\":\"55\"}}";
        String sticker = "{\"from\":\"1\",\"id\":\"wamid.sticker\",\"timestamp\":\"1792411170\","
                + "\"type\":\"sticker\"}";
        String texto = "{\"from\":\"1\",\"id\":\"wamid.texto\",\"timestamp\":\"1792411170\",\"type\":\"text\","
                + "\"text\":{\"body\":\"Hola\"}}";

        RecibirMensajesWhatsappRequest result = lectorWebhookMeta.leer(
                bytes(aviso(foto + "," + sticker + "," + texto)));

        assertThat(result.mensajes()).extracting(MensajeEntrante::waMensajeId).containsExactly("wamid.texto");
    }

    @Test
    @DisplayName("Un aviso de entrega o lectura no trae mensajes")
    void leerShouldIgnoreStatusUpdates() {
        RecibirMensajesWhatsappRequest result = lectorWebhookMeta.leer(bytes(AVISO_ESTADO));

        assertThat(result.mensajes()).isEmpty();
    }

    @Test
    @DisplayName("Lee todos los mensajes del aviso en orden, aunque vengan en varias entradas")
    void leerShouldReadEveryEntryInOrder() {
        String dosEntradas = "{\"entry\":["
                + AVISO_TEXTO.substring(AVISO_TEXTO.indexOf("[") + 1, AVISO_TEXTO.lastIndexOf("]"))
                + ","
                + AVISO_AUDIO.substring(AVISO_AUDIO.indexOf("[") + 1, AVISO_AUDIO.lastIndexOf("]"))
                + "]}";

        RecibirMensajesWhatsappRequest result = lectorWebhookMeta.leer(bytes(dosEntradas));

        assertThat(result.mensajes()).extracting(MensajeEntrante::waMensajeId)
                .containsExactly("wamid.texto", "wamid.audio");
    }

    @Test
    @DisplayName("Un cuerpo vacío o que no es JSON no trae mensajes")
    void leerShouldReturnNothingForInvalidJson() {
        assertThat(lectorWebhookMeta.leer(bytes("esto no es json")).mensajes()).isEmpty();
        assertThat(lectorWebhookMeta.leer(new byte[0]).mensajes()).isEmpty();
        assertThat(lectorWebhookMeta.leer(bytes("{}")).mensajes()).isEmpty();
    }
}
