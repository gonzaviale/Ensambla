package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefonoWhatsappIngresoManualTest {

    @Test
    @DisplayName("Deja sólo los dígitos de un número escrito con +, espacios, guiones y paréntesis")
    void deIngresoManualShouldKeepOnlyDigits() {
        assertThat(TelefonoWhatsapp.deIngresoManual("+54 9 (11) 2233-4455").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("A un celular de Argentina escrito sin el 9 se lo agrega, como lo identifica WhatsApp")
    void deIngresoManualShouldAddTheArgentineNine() {
        assertThat(TelefonoWhatsapp.deIngresoManual("+54 11 2233 4455").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Un número de otro país o que ya trae el 9 no se toca")
    void deIngresoManualShouldNotTouchOtherNumbers() {
        assertThat(TelefonoWhatsapp.deIngresoManual("5491122334455").valor()).isEqualTo("5491122334455");
        assertThat(TelefonoWhatsapp.deIngresoManual("+598 99 123 456").valor()).isEqualTo("59899123456");
        assertThat(TelefonoWhatsapp.deIngresoManual("541122334455").valor()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Un número nulo, con pocos dígitos o sin dígitos no es válido")
    void deIngresoManualShouldRejectInvalidNumbers() {
        assertThatThrownBy(() -> TelefonoWhatsapp.deIngresoManual(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> TelefonoWhatsapp.deIngresoManual("12345")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> TelefonoWhatsapp.deIngresoManual("abc")).isInstanceOf(BusinessRuleException.class);
    }
}
