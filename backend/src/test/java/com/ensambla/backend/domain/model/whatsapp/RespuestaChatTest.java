package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RespuestaChatTest {

    @ParameterizedTest
    @ValueSource(strings = {"SÍ", " si ", "Dale!", "ok"})
    @DisplayName("Reconoce un sí con o sin tilde, en mayúsculas o con signos")
    void reconoceUnSi(String escrito) {
        entoncesLaRespuestaA(escrito, RespuestaChat.SI);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO", "cancelar", "Descartar."})
    @DisplayName("Reconoce un no, cancelar o descartar")
    void reconoceUnNo(String escrito) {
        entoncesLaRespuestaA(escrito, RespuestaChat.NO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", " 12 "})
    @DisplayName("Reconoce un número de una o dos cifras como la elección de una opción")
    void reconoceUnNumeroComoEleccion(String escrito) {
        entoncesLaRespuestaA(escrito, RespuestaChat.NUMERO);
    }

    @Test
    @DisplayName("Lee el número elegido sin los espacios")
    void leeElNumeroElegidoSinLosEspacios() {
        entoncesElNumeroElegidoEn(" 12 ", 12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gasté 5000 en harina", "5000", ""})
    @DisplayName("Cualquier otra cosa es un mensaje nuevo, incluso un número largo")
    void cualquierOtraCosaEsUnMensajeNuevo(String escrito) {
        entoncesLaRespuestaA(escrito, RespuestaChat.OTRO);
    }

    private static void entoncesLaRespuestaA(String escrito, RespuestaChat esperada) {
        assertThat(RespuestaChat.de(escrito)).isEqualTo(esperada);
    }

    private static void entoncesElNumeroElegidoEn(String escrito, int numero) {
        assertThat(RespuestaChat.numero(escrito)).isEqualTo(numero);
    }
}