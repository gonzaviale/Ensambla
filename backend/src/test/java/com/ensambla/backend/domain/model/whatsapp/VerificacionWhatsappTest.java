package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificacionWhatsappTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final TelefonoWhatsapp TELEFONO = new TelefonoWhatsapp("5491122334455");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-27T12:00:00Z");

    @Test
    @DisplayName("Nueva guarda la huella del código, no el código, y vence a los 10 minutos")
    void nuevaShouldStoreHashAndExpiry() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThat(verificacion.getCodigoHash()).isEqualTo(VerificacionWhatsapp.huella("123456"))
                .doesNotContain("123456");
        assertThat(verificacion.getExpiraEn()).isEqualTo(AHORA.plusMinutes(10));
        assertThat(verificacion.reenviarDesde()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(verificacion.getTelefono()).isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Confirmar con el código correcto no lanza error aunque tenga espacios")
    void confirmarShouldAcceptRightCode() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThatCode(() -> verificacion.confirmar(" 123456 ", AHORA.plusMinutes(1))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Confirmar con un código incorrecto cuenta el intento y dice cuántos quedan")
    void confirmarShouldCountWrongAttempts() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThatThrownBy(() -> verificacion.confirmar("000000", AHORA.plusMinutes(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El código no es correcto. Te quedan 4 intentos");
        assertThat(verificacion.getIntentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Confirmar después de 5 intentos fallidos pide un código nuevo aunque el código sea el correcto")
    void confirmarShouldRejectAfterMaxAttempts() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");
        for (int i = 0; i < VerificacionWhatsapp.MAX_INTENTOS; i++) {
            assertThatThrownBy(() -> verificacion.confirmar("000000", AHORA.plusMinutes(1)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        assertThatThrownBy(() -> verificacion.confirmar("123456", AHORA.plusMinutes(1)))
                .hasMessage("Hiciste demasiados intentos. Pedí un código nuevo");
    }

    @Test
    @DisplayName("Confirmar con el código vencido pide uno nuevo")
    void confirmarShouldRejectExpiredCode() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThatThrownBy(() -> verificacion.confirmar("123456", AHORA.plusMinutes(10)))
                .hasMessage("El código venció. Pedí uno nuevo");
    }

    @Test
    @DisplayName("Reenviar antes de que pasen 60 segundos pide esperar")
    void reenviarShouldWaitBetweenSends() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");

        assertThatThrownBy(() -> verificacion.reenviar(TELEFONO, "654321", AHORA.plusSeconds(30)))
                .hasMessage("Esperá un momento antes de pedir otro código");
    }

    @Test
    @DisplayName("Reenviar reemplaza el código y el número, reinicia los intentos y cuenta el envío")
    void reenviarShouldReplaceCodeAndPhone() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");
        assertThatThrownBy(() -> verificacion.confirmar("000000", AHORA.plusSeconds(10)));

        verificacion.reenviar(new TelefonoWhatsapp("5491100000000"), "654321", AHORA.plusSeconds(61));

        assertThat(verificacion.getIntentos()).isZero();
        assertThat(verificacion.getTelefono()).isEqualTo("5491100000000");
        assertThat(verificacion.getCodigoHash()).isEqualTo(VerificacionWhatsapp.huella("654321"));
        assertThat(verificacion.getEnviosVentana()).isEqualTo(2);
        assertThat(verificacion.getExpiraEn()).isEqualTo(AHORA.plusSeconds(61).plusMinutes(10));
    }

    @Test
    @DisplayName("Reenviar más de 5 veces en una hora se rechaza, y pasada la hora vuelve a permitirse")
    void reenviarShouldLimitSendsPerHour() {
        VerificacionWhatsapp verificacion = givenVerificacion("123456");
        OffsetDateTime momento = AHORA;
        for (int i = 1; i < VerificacionWhatsapp.MAX_ENVIOS_POR_VENTANA; i++) {
            momento = momento.plusSeconds(61);
            verificacion.reenviar(TELEFONO, "111111", momento);
        }
        OffsetDateTime siguiente = momento.plusSeconds(61);

        assertThatThrownBy(() -> verificacion.reenviar(TELEFONO, "222222", siguiente))
                .hasMessage("Pediste demasiados códigos. Probá de nuevo en una hora");
        verificacion.reenviar(TELEFONO, "333333", AHORA.plusHours(1).plusSeconds(1));
        assertThat(verificacion.getEnviosVentana()).isEqualTo(1);
    }

    private static VerificacionWhatsapp givenVerificacion(String codigo) {
        return VerificacionWhatsapp.nueva(USUARIO_ID, TELEFONO, codigo, AHORA);
    }
}
