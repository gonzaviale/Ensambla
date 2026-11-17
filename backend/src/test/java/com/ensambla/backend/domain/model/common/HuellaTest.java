package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HuellaTest {

    private String huella;
    private String otraHuella;

    @Test
    @DisplayName("La huella es el UUID derivado del texto en UTF-8")
    void laHuellaEsElUuidDerivadoDelTextoEnUtf8() {
        cuandoSeCalculaLaHuellaDe("Vela de soja|Ñandú");

        entoncesEsElUuidDerivadoDelTextoEnUtf8("Vela de soja|Ñandú");
    }

    @Test
    @DisplayName("El mismo texto da siempre la misma huella")
    void elMismoTextoDaSiempreLaMismaHuella() {
        cuandoSeCalculaLaHuellaDeDosTextos("stock=3", "stock=3");

        entoncesLasHuellasSonIguales();
    }

    @Test
    @DisplayName("Textos distintos dan huellas distintas")
    void textosDistintosDanHuellasDistintas() {
        cuandoSeCalculaLaHuellaDeDosTextos("stock=3", "stock=4");

        entoncesLasHuellasSonDistintas();
    }

    // --- cuando ---
    private void cuandoSeCalculaLaHuellaDe(String texto) {
        huella = Huella.de(texto);
    }

    private void cuandoSeCalculaLaHuellaDeDosTextos(String texto, String otroTexto) {
        huella = Huella.de(texto);
        otraHuella = Huella.de(otroTexto);
    }

    // --- entonces ---
    private void entoncesEsElUuidDerivadoDelTextoEnUtf8(String texto) {
        assertThat(huella).isEqualTo(UUID.nameUUIDFromBytes(texto.getBytes(StandardCharsets.UTF_8)).toString());
    }

    private void entoncesLasHuellasSonIguales() {
        assertThat(huella).isEqualTo(otraHuella);
    }

    private void entoncesLasHuellasSonDistintas() {
        assertThat(huella).isNotEqualTo(otraHuella);
    }
}
