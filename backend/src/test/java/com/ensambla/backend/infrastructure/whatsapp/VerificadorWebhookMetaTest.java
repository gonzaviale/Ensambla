package com.ensambla.backend.infrastructure.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.APP_SECRET;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.AVISO_TEXTO;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.PROPERTIES;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.VERIFY_TOKEN;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.bytes;
import static com.ensambla.backend.infrastructure.whatsapp.WhatsappWebhookTestData.firma;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorWebhookMetaTest {

    private static final byte[] CUERPO = bytes(AVISO_TEXTO);

    private final VerificadorWebhookMeta verificadorWebhookMeta = new VerificadorWebhookMeta(PROPERTIES);
    private final VerificadorWebhookMeta sinConfigurar = new VerificadorWebhookMeta(new WhatsappProperties("", ""));

    @Test
    @DisplayName("Acepta la suscripción con el token configurado")
    void suscripcionValidaShouldAcceptTheConfiguredToken() {
        boolean result = verificadorWebhookMeta.suscripcionValida("subscribe", VERIFY_TOKEN);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Rechaza la suscripción con otro token o sin token")
    void suscripcionValidaShouldRejectOtherToken() {
        assertThat(verificadorWebhookMeta.suscripcionValida("subscribe", "otro")).isFalse();
        assertThat(verificadorWebhookMeta.suscripcionValida("subscribe", null)).isFalse();
    }

    @Test
    @DisplayName("Rechaza la suscripción si el modo no es subscribe")
    void suscripcionValidaShouldRejectOtherMode() {
        boolean result = verificadorWebhookMeta.suscripcionValida("unsubscribe", VERIFY_TOKEN);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Sin token configurado no acepta ninguna suscripción, ni siquiera con el token vacío")
    void suscripcionValidaShouldRejectEverythingWithoutConfiguredToken() {
        boolean result = sinConfigurar.suscripcionValida("subscribe", "");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Acepta un aviso firmado con el secreto de la app")
    void firmaValidaShouldAcceptTheMetaSignature() throws GeneralSecurityException {
        boolean result = verificadorWebhookMeta.firmaValida(CUERPO, firma(CUERPO, APP_SECRET));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Rechaza un aviso firmado con otro secreto")
    void firmaValidaShouldRejectOtherSecret() throws GeneralSecurityException {
        boolean result = verificadorWebhookMeta.firmaValida(CUERPO, firma(CUERPO, "otro-secreto"));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Rechaza un aviso cambiado después de firmarlo")
    void firmaValidaShouldRejectAlteredBody() throws GeneralSecurityException {
        String firmaOriginal = firma(CUERPO, APP_SECRET);

        boolean result = verificadorWebhookMeta.firmaValida(bytes(AVISO_TEXTO.replace("4500", "1")), firmaOriginal);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Rechaza un aviso sin firma o con una firma mal escrita")
    void firmaValidaShouldRejectMissingOrMalformedSignature() throws GeneralSecurityException {
        assertThat(verificadorWebhookMeta.firmaValida(CUERPO, null)).isFalse();
        assertThat(verificadorWebhookMeta.firmaValida(CUERPO, firma(CUERPO, APP_SECRET).substring(7))).isFalse();
        assertThat(verificadorWebhookMeta.firmaValida(CUERPO, "sha256=zz")).isFalse();
    }

    @Test
    @DisplayName("Sin secreto configurado no acepta ninguna firma")
    void firmaValidaShouldRejectEverythingWithoutConfiguredSecret() throws GeneralSecurityException {
        boolean result = sinConfigurar.firmaValida(CUERPO, firma(CUERPO, APP_SECRET));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Si la JVM no tiene el algoritmo falla en lugar de aceptar el aviso")
    void firmaValidaShouldFailWhenAlgorithmIsMissing() {
        VerificadorWebhookMeta sinAlgoritmo = new VerificadorWebhookMeta(PROPERTIES, "HmacNoExiste");

        assertThatThrownBy(() -> sinAlgoritmo.firmaValida(CUERPO, "sha256=00"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo calcular la firma del webhook de WhatsApp");
    }
}
