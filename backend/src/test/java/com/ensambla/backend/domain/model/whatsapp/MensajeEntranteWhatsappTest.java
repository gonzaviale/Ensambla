package com.ensambla.backend.domain.model.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.TEXTO;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static com.ensambla.backend.support.WhatsappTestData.texto;
import static org.assertj.core.api.Assertions.assertThat;

class MensajeEntranteWhatsappTest {

    @ParameterizedTest
    @ValueSource(strings = {"482913", " 482913 ", "Ensambla 482913", "ENSAMBLA   482913", "ensambla 482913\n"})
    @DisplayName("Reconoce el código solo o después de la palabra Ensambla")
    void codigoDeVinculacionShouldReadTheCodigo(String mensaje) {
        Optional<String> result = texto(mensaje).codigoDeVinculacion();

        assertThat(result).contains(CODIGO);
    }

    @ParameterizedTest
    @ValueSource(strings = {TEXTO, "48291", "4829131", "mi código es 482913", "482913 gracias", ""})
    @DisplayName("Un texto que no es sólo el código no vincula nada")
    void codigoDeVinculacionShouldBeEmptyForOtherTexts(String mensaje) {
        Optional<String> result = texto(mensaje).codigoDeVinculacion();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Una nota de voz nunca es un código")
    void codigoDeVinculacionShouldBeEmptyForAudio() {
        Optional<String> result = audio().codigoDeVinculacion();

        assertThat(result).isEmpty();
    }
}
