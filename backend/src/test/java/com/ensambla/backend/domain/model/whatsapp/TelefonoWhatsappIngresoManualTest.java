package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TelefonoWhatsappIngresoManualTest {

    @Test
    @DisplayName("Deja sólo los dígitos de un número escrito con +, espacios, guiones y paréntesis")
    void dejaSoloLosDigitos() {
        entoncesElNumeroCargadoQueda("+54 9 (11) 2233-4455", "5491122334455");
    }

    @Test
    @DisplayName("A un celular de Argentina escrito sin el 9 se lo agrega, como lo identifica WhatsApp")
    void aUnCelularArgentinoSinElNueveSeLoAgrega() {
        entoncesElNumeroCargadoQueda("+54 11 2233 4455", "5491122334455");
    }

    @ParameterizedTest
    @CsvSource({"5491122334455,5491122334455", "+598 99 123 456,59899123456", "541122334455,5491122334455"})
    @DisplayName("Un número de otro país o que ya trae el 9 no se toca")
    void unNumeroDeOtroPaisOQueYaTraeElNueveNoSeToca(String escrito, String queda) {
        entoncesElNumeroCargadoQueda(escrito, queda);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"12345", "abc"})
    @DisplayName("Un número nulo, con pocos dígitos o sin dígitos no es válido")
    void unNumeroNuloCortoOSinDigitosNoEsValido(String escrito) {
        entoncesElNumeroCargadoSeRechaza(escrito);
    }

    private static void entoncesElNumeroCargadoQueda(String escrito, String queda) {
        assertThat(TelefonoWhatsapp.deIngresoManual(escrito).valor()).isEqualTo(queda);
    }

    private static void entoncesElNumeroCargadoSeRechaza(String escrito) {
        assertThat(catchThrowable(() -> TelefonoWhatsapp.deIngresoManual(escrito)))
                .isInstanceOf(BusinessRuleException.class);
    }
}