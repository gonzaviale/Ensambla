package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextoSeguroTest {

    private static final String TEXTO_SIN_DATOS_PERSONALES = "Quiero vender 20 budines por semana";

    private String resultado;

    @Test
    @DisplayName("Oculta los correos electrónicos")
    void deShouldHideEmails() {
        whenSeProtege("Escribile a juan.perez+tienda@mail.com.ar hoy");

        thenElTextoEs("Escribile a [dato oculto] hoy");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Oculta los teléfonos con espacios, guiones y prefijo")
    @CsvSource(delimiter = '|', textBlock = """
            Llamalo al 11 5555 5555 | Llamalo al [dato oculto]
            o al +54 9 11 5555-5555 | o al [dato oculto]
            """)
    void deShouldHidePhoneNumbers(String texto, String esperado) {
        whenSeProtege(texto);

        thenElTextoEs(esperado);
    }

    @Test
    @DisplayName("Oculta un DNI con puntos")
    void deShouldHideDocumentNumbers() {
        whenSeProtege("Su DNI es 12.345.678");

        thenElTextoEs("Su DNI es [dato oculto]");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("No toca un importe en pesos")
    @CsvSource(delimiter = '|', textBlock = """
            Quiero ganar $1.400.000 por mes
            o $400.000
            """)
    void deShouldKeepAmounts(String texto) {
        whenSeProtege(texto);

        thenElTextoEs(texto);
    }

    @Test
    @DisplayName("No toca un texto sin datos personales")
    void deShouldKeepPlainText() {
        whenSeProtege(TEXTO_SIN_DATOS_PERSONALES);

        thenElTextoEs(TEXTO_SIN_DATOS_PERSONALES);
    }

    // --- act ---
    private void whenSeProtege(String texto) {
        resultado = TextoSeguro.de(texto);
    }

    // --- assert ---
    private void thenElTextoEs(String esperado) {
        assertThat(resultado).isEqualTo(esperado);
    }
}
