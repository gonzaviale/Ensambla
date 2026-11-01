package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RespuestaChatTest {

    @Test
    @DisplayName("Reconoce un sí con o sin tilde, en mayúsculas o con signos")
    void deShouldRecognizeYes() {
        assertThat(RespuestaChat.de("SÍ")).isEqualTo(RespuestaChat.SI);
        assertThat(RespuestaChat.de(" si ")).isEqualTo(RespuestaChat.SI);
        assertThat(RespuestaChat.de("Dale!")).isEqualTo(RespuestaChat.SI);
        assertThat(RespuestaChat.de("ok")).isEqualTo(RespuestaChat.SI);
    }

    @Test
    @DisplayName("Reconoce un no, cancelar o descartar")
    void deShouldRecognizeNo() {
        assertThat(RespuestaChat.de("NO")).isEqualTo(RespuestaChat.NO);
        assertThat(RespuestaChat.de("cancelar")).isEqualTo(RespuestaChat.NO);
        assertThat(RespuestaChat.de("Descartar.")).isEqualTo(RespuestaChat.NO);
    }

    @Test
    @DisplayName("Reconoce un número de una o dos cifras como la elección de una opción")
    void deShouldRecognizeNumber() {
        assertThat(RespuestaChat.de("2")).isEqualTo(RespuestaChat.NUMERO);
        assertThat(RespuestaChat.de(" 12 ")).isEqualTo(RespuestaChat.NUMERO);
        assertThat(RespuestaChat.numero(" 12 ")).isEqualTo(12);
    }

    @Test
    @DisplayName("Cualquier otra cosa es un mensaje nuevo, incluso un número largo")
    void deShouldTreatAnythingElseAsNewMessage() {
        assertThat(RespuestaChat.de("gasté 5000 en harina")).isEqualTo(RespuestaChat.OTRO);
        assertThat(RespuestaChat.de("5000")).isEqualTo(RespuestaChat.OTRO);
        assertThat(RespuestaChat.de("")).isEqualTo(RespuestaChat.OTRO);
    }
}
