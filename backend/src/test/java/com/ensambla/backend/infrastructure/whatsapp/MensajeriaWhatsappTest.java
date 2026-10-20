package com.ensambla.backend.infrastructure.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MensajeriaWhatsappTest {

    private static final String BASE = "http://evolution.test";

    @Test
    @DisplayName("Evolution recibe el texto en la instancia, con la clave y el número destino")
    void enviarTextoShouldPostToEvolutionInstance() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("apikey", "clave");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/message/sendText/ensambla"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "clave"))
                .andExpect(content().json("{\"number\":\"5491122334455\",\"text\":\"hola\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new ClienteEvolutionApi(builder.build(), "ensambla", "5491100000000").enviarTexto("5491122334455", "hola");

        server.verify();
    }

    @Test
    @DisplayName("Si Evolution falla no se propaga el error")
    void enviarTextoShouldSwallowProviderErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/message/sendText/ensambla")).andRespond(withServerError());

        new ClienteEvolutionApi(builder.build(), "ensambla", "").enviarTexto("5491122334455", "hola");

        server.verify();
    }

    @Test
    @DisplayName("El cliente de Evolution informa el número del bot configurado")
    void numeroBotShouldReturnConfiguredNumber() {
        ClienteEvolutionApi cliente = new ClienteEvolutionApi(RestClient.create(BASE), "ensambla", "5491100000000");

        assertThat(cliente.numeroBot()).isEqualTo("5491100000000");
    }

    @Test
    @DisplayName("El cliente simulado guarda lo que habría enviado")
    void simuladaShouldKeepSentMessages() {
        MensajeriaWhatsappSimulada simulada = new MensajeriaWhatsappSimulada("5491100000000");

        simulada.enviarTexto("5491122334455", "hola");

        assertThat(simulada.enviados())
                .containsExactly(new MensajeriaWhatsappSimulada.Enviado("5491122334455", "hola"));
        assertThat(simulada.numeroBot()).isEqualTo("5491100000000");
    }

    @Test
    @DisplayName("La configuración arma el cliente simulado o el de Evolution según el proveedor")
    void configShouldBuildBothProviders() {
        WhatsappProperties properties = new WhatsappProperties("evolution", "5491100000000", "secreto",
                new WhatsappProperties.Evolution("http://localhost:8081", "clave", "ensambla"));
        WhatsappConfig config = new WhatsappConfig();

        assertThat(config.mensajeriaWhatsappSimulada(properties)).isInstanceOf(MensajeriaWhatsappSimulada.class);
        assertThat(config.mensajeriaWhatsappEvolution(properties)).isInstanceOf(ClienteEvolutionApi.class);
        assertThat(config.evolutionWebhookParser(Clock.systemUTC())).isNotNull();
    }
}
