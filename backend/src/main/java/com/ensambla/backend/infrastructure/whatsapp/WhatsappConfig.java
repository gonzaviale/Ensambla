package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Cableado de la carga por WhatsApp. Para cambiar de proveedor (YCloud, la API oficial de Meta) hay que implementar
 * {@link MensajeriaWhatsappPort}, un parser del webhook de ese proveedor y registrarlo acá con
 * {@code @ConditionalOnProperty(prefix = "ensambla.whatsapp", name = "proveedor", havingValue = "<nombre>")}.
 */
@Configuration
@EnableConfigurationProperties(WhatsappProperties.class)
public class WhatsappConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.whatsapp", name = "proveedor", havingValue = "simulado",
            matchIfMissing = true)
    MensajeriaWhatsappPort mensajeriaWhatsappSimulada(WhatsappProperties properties) {
        return new MensajeriaWhatsappSimulada(properties.numeroBot());
    }

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.whatsapp", name = "proveedor", havingValue = "evolution")
    MensajeriaWhatsappPort mensajeriaWhatsappEvolution(WhatsappProperties properties) {
        WhatsappProperties.Evolution evolution = properties.evolution();
        RestClient restClient = RestClient.builder()
                .baseUrl(evolution.url())
                .defaultHeader("apikey", evolution.apiKey())
                .build();
        return new ClienteEvolutionApi(restClient, evolution.instancia(), properties.numeroBot());
    }

    @Bean
    EvolutionWebhookParser evolutionWebhookParser(Clock clock) {
        return new EvolutionWebhookParser(clock);
    }
}
