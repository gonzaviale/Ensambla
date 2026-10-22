package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MercadoPagoProperties.class)
public class MercadoPagoConfig {

    @Bean
    public PasarelaSuscripcionesPort pasarelaSuscripcionesPort(MercadoPagoProperties properties,
                                                              ObjectMapper objectMapper) {
        if (properties.isHabilitado() && (properties.getAccessToken().isBlank()
                || properties.getCollectorId().isBlank() || properties.getApplicationId().isBlank()
                || !properties.getReturnUrl().startsWith("https://")
                || (!properties.getNotificationUrl().isBlank()
                    && (!properties.getNotificationUrl().startsWith("https://")
                        || properties.getWebhookSecret().isBlank())))) {
            throw new IllegalStateException("Completá la configuración privada de Mercado Pago antes de habilitarlo.");
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new MercadoPagoSuscripcionesAdapter(properties, objectMapper, httpClient);
    }
}
