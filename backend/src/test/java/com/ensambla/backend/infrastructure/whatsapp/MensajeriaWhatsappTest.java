package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Base64;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("enviarTexto avisa si Evolution lo entregó o no")
    void enviarTextoShouldReportDelivery() {
        RestClient.Builder bueno = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(bueno).build();
        servidor.expect(requestTo(BASE + "/message/sendText/ensambla")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        RestClient.Builder malo = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer rechazo = MockRestServiceServer.bindTo(malo).build();
        rechazo.expect(requestTo(BASE + "/message/sendText/ensambla")).andRespond(withServerError());

        assertThat(new ClienteEvolutionApi(bueno.build(), "ensambla", "").enviarTexto("5491122334455", "hola")).isTrue();
        assertThat(new ClienteEvolutionApi(malo.build(), "ensambla", "").enviarTexto("5491122334455", "hola")).isFalse();
    }

    @Test
    @DisplayName("descargarMedia pide la foto por el id del mensaje y decodifica el base64")
    void descargarMediaShouldDecodeTheFile() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/chat/getBase64FromMediaMessage/ensambla"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"message\":{\"key\":{\"id\":\"F1\"}},\"convertToMp4\":false}"))
                .andRespond(withSuccess("{\"base64\":\"/9j/AAAA\",\"mimetype\":\"image/png\"}",
                        MediaType.APPLICATION_JSON));

        ArchivoWhatsapp archivo = new ClienteEvolutionApi(builder.build(), "ensambla", "").descargarMedia("F1");

        assertThat(archivo.contenido()).isEqualTo(Base64.getDecoder().decode("/9j/AAAA"));
        assertThat(archivo.mime()).isEqualTo("image/png");
        server.verify();
    }

    @Test
    @DisplayName("descargarMedia sin tipo informado asume JPEG")
    void descargarMediaShouldDefaultToJpeg() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer.bindTo(builder).build().expect(requestTo(BASE + "/chat/getBase64FromMediaMessage/ensambla"))
                .andRespond(withSuccess("{\"base64\":\"/9j/AAAA\",\"mimetype\":\" \"}", MediaType.APPLICATION_JSON));

        assertThat(new ClienteEvolutionApi(builder.build(), "ensambla", "").descargarMedia("F1").mime())
                .isEqualTo("image/jpeg");
    }


    @Test
    @DisplayName("descargarMedia sin la clave de tipo asume JPEG")
    void descargarMediaShouldDefaultWhenMimeKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer.bindTo(builder).build().expect(requestTo(BASE + "/chat/getBase64FromMediaMessage/ensambla"))
                .andRespond(withSuccess("{\"base64\":\"/9j/AAAA\"}", MediaType.APPLICATION_JSON));

        assertThat(new ClienteEvolutionApi(builder.build(), "ensambla", "").descargarMedia("F1").mime())
                .isEqualTo("image/jpeg");
    }
    @Test
    @DisplayName("descargarMedia falla si Evolution no devuelve el archivo")
    void descargarMediaShouldFailWithoutFile() {
        for (String cuerpo : new String[]{"{}", "{\"base64\":\"\"}", "null"}) {
            RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
            MockRestServiceServer.bindTo(builder).build().expect(requestTo(BASE + "/chat/getBase64FromMediaMessage/ensambla"))
                    .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON));
            ClienteEvolutionApi cliente = new ClienteEvolutionApi(builder.build(), "ensambla", "");

            assertThatThrownBy(() -> cliente.descargarMedia("F1")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("El cliente simulado falla los envíos a pedido y baja la foto cargada o una de ejemplo")
    void simuladaShouldSupportTestHooks() {
        MensajeriaWhatsappSimulada simulada = new MensajeriaWhatsappSimulada("");
        simulada.fallarEnvios(true);
        boolean fallido = simulada.enviarTexto("5491122334455", "hola");
        simulada.fallarEnvios(false);
        ArchivoWhatsapp propia = new ArchivoWhatsapp(new byte[]{1}, "image/png");
        simulada.ponerMedia("F1", propia);

        assertThat(fallido).isFalse();
        assertThat(simulada.enviados()).isEmpty();
        assertThat(simulada.descargarMedia("F1")).isSameAs(propia);
        assertThat(simulada.descargarMedia("otra").mime()).isEqualTo("image/jpeg");
        assertThat(simulada.descargarMedia("otra").contenido()).isNotEmpty();
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
                new WhatsappProperties.Evolution("http://localhost:8081", "clave", "ensambla", Duration.ofSeconds(5)),
                new WhatsappProperties.Atencion(false, Duration.ofSeconds(1), Duration.ofMinutes(2), 0.6));
        WhatsappConfig config = new WhatsappConfig();

        assertThat(config.mensajeriaWhatsappSimulada(properties)).isInstanceOf(MensajeriaWhatsappSimulada.class);
        assertThat(config.mensajeriaWhatsappEvolution(properties)).isInstanceOf(ClienteEvolutionApi.class);
        assertThat(config.evolutionWebhookParser(Clock.systemUTC())).isNotNull();
    }
}
