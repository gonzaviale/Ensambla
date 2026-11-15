package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoSeguroTest {

    @Test
    @DisplayName("Oculta los correos electrónicos")
    void deShouldHideEmails() {
        assertThat(TextoSeguro.de("Escribile a juan.perez+tienda@mail.com.ar hoy"))
                .isEqualTo("Escribile a [dato oculto] hoy");
    }

    @Test
    @DisplayName("Oculta los teléfonos con espacios, guiones y prefijo")
    void deShouldHidePhoneNumbers() {
        assertThat(TextoSeguro.de("Llamalo al 11 5555 5555")).isEqualTo("Llamalo al [dato oculto]");
        assertThat(TextoSeguro.de("o al +54 9 11 5555-5555")).isEqualTo("o al [dato oculto]");
    }

    @Test
    @DisplayName("Oculta un DNI con puntos")
    void deShouldHideDocumentNumbers() {
        assertThat(TextoSeguro.de("Su DNI es 12.345.678")).isEqualTo("Su DNI es [dato oculto]");
    }

    @Test
    @DisplayName("No toca un importe en pesos")
    void deShouldKeepAmounts() {
        assertThat(TextoSeguro.de("Quiero ganar $1.400.000 por mes")).isEqualTo("Quiero ganar $1.400.000 por mes");
        assertThat(TextoSeguro.de("o $400.000")).isEqualTo("o $400.000");
    }

    @Test
    @DisplayName("No toca un texto sin datos personales")
    void deShouldKeepPlainText() {
        String texto = "Quiero vender 20 budines por semana";

        assertThat(TextoSeguro.de(texto)).isEqualTo(texto);
    }
}
