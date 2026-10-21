package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MercadoPagoConfigTest {

    @Test
    @DisplayName("Sin claves la aplicación arranca con cobros deshabilitados")
    void pasarelaSuscripcionesPortShouldStayDisabledByDefault() {
        PasarelaSuscripcionesPort pasarela = new MercadoPagoConfig()
                .pasarelaSuscripcionesPort(new MercadoPagoProperties(), new ObjectMapper());

        assertThat(pasarela.disponible()).isFalse();
    }

    @Test
    @DisplayName("Una configuración completa registra el adaptador sin efectuar llamadas externas al arrancar")
    void pasarelaSuscripcionesPortShouldWireConfiguredAdapter() {
        PasarelaSuscripcionesPort pasarela = new MercadoPagoConfig().pasarelaSuscripcionesPort(
                MercadoPagoSuscripcionesAdapterTest.propiedades(), new ObjectMapper());

        assertThat(pasarela.disponible()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("No habilita los cobros con secretos, cuenta o URLs incompletos")
    void pasarelaSuscripcionesPortShouldRejectIncompleteEnabledConfiguration(int caso) {
        MercadoPagoProperties properties = MercadoPagoSuscripcionesAdapterTest.propiedades();
        switch (caso) {
            case 0 -> properties.setAccessToken("");
            case 1 -> properties.setCollectorId("");
            case 2 -> properties.setApplicationId("");
            case 3 -> properties.setWebhookSecret("");
            case 4 -> properties.setNotificationUrl("http://localhost/webhook");
            case 5 -> properties.setReturnUrl("http://localhost/planes");
            default -> throw new IllegalArgumentException();
        }

        assertThatThrownBy(() -> new MercadoPagoConfig().pasarelaSuscripcionesPort(properties, new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("La conciliación autenticada puede funcionar sin habilitar notificaciones no firmadas")
    void pasarelaSuscripcionesPortShouldAllowReconciliationWithoutWebhookConfiguration() {
        MercadoPagoProperties properties = MercadoPagoSuscripcionesAdapterTest.propiedades();
        properties.setNotificationUrl("");
        properties.setWebhookSecret("");

        PasarelaSuscripcionesPort pasarela = new MercadoPagoConfig().pasarelaSuscripcionesPort(properties,
                new ObjectMapper());

        assertThat(pasarela.disponible()).isTrue();
    }
}
