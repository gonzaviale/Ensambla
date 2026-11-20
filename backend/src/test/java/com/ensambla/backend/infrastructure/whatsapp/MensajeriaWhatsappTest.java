package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MensajeriaWhatsappTest {

    private static final String BASE = "http://evolution.test";
    private static final String ENVIAR_TEXTO = "/message/sendText/ensambla";
    private static final String DESCARGAR_MEDIA = "/chat/getBase64FromMediaMessage/ensambla";

    private MockRestServiceServer server;
    private ClienteEvolutionApi cliente;
    private MensajeriaWhatsappSimulada simulada;
    private ArchivoWhatsapp propia;
    private ArchivoWhatsapp archivo;
    private boolean entregado;
    private Throwable fallo;
    private WhatsappConfig config;
    private WhatsappProperties properties;

    @Test
    @DisplayName("Evolution recibe el texto en la instancia, con la clave y el número destino")
    void evolutionRecibeElTextoEnLaInstanciaConLaClaveYElNumeroDestino() {
        dadoUnEvolutionQueEsperaElTextoConLaClave();

        cuandoSeEnvia("hola");

        entoncesEvolutionRecibioLoEsperado();
    }

    @Test
    @DisplayName("Si Evolution falla no se propaga el error")
    void siEvolutionFallaNoSePropagaElError() {
        dadoUnEvolutionQueResponde(ENVIAR_TEXTO, withServerError());

        cuandoSeEnvia("hola");

        entoncesEvolutionRecibioLoEsperado();
    }

    @Test
    @DisplayName("enviarTexto avisa que Evolution lo entregó")
    void enviarTextoAvisaQueEvolutionLoEntrego() {
        dadoUnEvolutionQueResponde(ENVIAR_TEXTO, withSuccess("{}", MediaType.APPLICATION_JSON));

        cuandoSeEnvia("hola");

        entoncesSeEntrego();
    }

    @Test
    @DisplayName("enviarTexto avisa que Evolution no lo entregó")
    void enviarTextoAvisaQueEvolutionNoLoEntrego() {
        dadoUnEvolutionQueResponde(ENVIAR_TEXTO, withServerError());

        cuandoSeEnvia("hola");

        entoncesNoSeEntrego();
    }

    @Test
    @DisplayName("descargarMedia pide la foto por el id del mensaje y decodifica el base64")
    void descargarMediaPideLaFotoPorElIdDelMensajeYDecodificaElBase64() {
        dadoUnEvolutionQueEntregaUnaFotoPng();

        cuandoSeDescargaLaMedia();

        entoncesSeDecodificaLaFotoPng();
    }

    @Test
    @DisplayName("descargarMedia sin tipo informado asume JPEG")
    void descargarMediaSinTipoInformadoAsumeJpeg() {
        dadoUnEvolutionQueResponde(DESCARGAR_MEDIA,
                withSuccess("{\"base64\":\"/9j/AAAA\",\"mimetype\":\" \"}", MediaType.APPLICATION_JSON));

        cuandoSeDescargaLaMedia();

        entoncesElTipoEs("image/jpeg");
    }

    @Test
    @DisplayName("descargarMedia sin la clave de tipo asume JPEG")
    void descargarMediaSinLaClaveDeTipoAsumeJpeg() {
        dadoUnEvolutionQueResponde(DESCARGAR_MEDIA,
                withSuccess("{\"base64\":\"/9j/AAAA\"}", MediaType.APPLICATION_JSON));

        cuandoSeDescargaLaMedia();

        entoncesElTipoEs("image/jpeg");
    }

    @Test
    @DisplayName("descargarMedia falla si Evolution devuelve un objeto vacío")
    void descargarMediaFallaSiEvolutionDevuelveUnObjetoVacio() {
        dadoUnEvolutionQueResponde(DESCARGAR_MEDIA, withSuccess("{}", MediaType.APPLICATION_JSON));

        cuandoSeIntentaDescargarLaMedia();

        entoncesFallaPorqueNoHayArchivo();
    }

    @Test
    @DisplayName("descargarMedia falla si Evolution devuelve el base64 vacío")
    void descargarMediaFallaSiEvolutionDevuelveElBase64Vacio() {
        dadoUnEvolutionQueResponde(DESCARGAR_MEDIA,
                withSuccess("{\"base64\":\"\"}", MediaType.APPLICATION_JSON));

        cuandoSeIntentaDescargarLaMedia();

        entoncesFallaPorqueNoHayArchivo();
    }

    @Test
    @DisplayName("descargarMedia falla si Evolution devuelve null")
    void descargarMediaFallaSiEvolutionDevuelveNull() {
        dadoUnEvolutionQueResponde(DESCARGAR_MEDIA, withSuccess("null", MediaType.APPLICATION_JSON));

        cuandoSeIntentaDescargarLaMedia();

        entoncesFallaPorqueNoHayArchivo();
    }

    @Test
    @DisplayName("El cliente simulado falla los envíos a pedido")
    void elClienteSimuladoFallaLosEnviosAPedido() {
        dadoUnClienteSimuladoQueFallaLosEnvios();

        cuandoSeEnviaConElClienteSimulado();

        entoncesElEnvioSimuladoFalloYNoQuedoRegistrado();
    }

    @Test
    @DisplayName("El cliente simulado baja la foto cargada")
    void elClienteSimuladoBajaLaFotoCargada() {
        dadoUnClienteSimuladoConUnaFotoCargada();

        cuandoSeDescargaLaFotoCargadaDelSimulado();

        entoncesSeBajaLaFotoCargada();
    }

    @Test
    @DisplayName("El cliente simulado baja una foto de ejemplo si no hay una cargada")
    void elClienteSimuladoBajaUnaFotoDeEjemploSiNoHayUnaCargada() {
        dadoUnClienteSimuladoSinFotos();

        cuandoSeDescargaUnaFotoNoCargadaDelSimulado();

        entoncesSeBajaUnaFotoDeEjemploJpeg();
    }

    @Test
    @DisplayName("El cliente de Evolution informa el número del bot configurado")
    void elClienteDeEvolutionInformaElNumeroDelBotConfigurado() {
        dadoUnClienteEvolutionConElNumeroDelBot();

        entoncesElNumeroDelBotEs("5491100000000");
    }

    @Test
    @DisplayName("El cliente simulado guarda lo que habría enviado")
    void elClienteSimuladoGuardaLoQueHabriaEnviado() {
        dadoUnClienteSimuladoConElNumeroDelBot();

        cuandoSeEnviaConElClienteSimulado();

        entoncesElSimuladoGuardoElEnvioYInformaElNumeroDelBot();
    }

    @Test
    @DisplayName("La configuración arma el cliente simulado o el de Evolution según el proveedor")
    void laConfiguracionArmaElClienteSimuladoOElDeEvolutionSegunElProveedor() {
        dadoLaConfiguracionDeWhatsapp();

        entoncesSeArmanAmbosProveedores();
    }

    // --- dado ---

    private void dadoUnEvolutionQueEsperaElTextoConLaClave() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("apikey", "clave");
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + ENVIAR_TEXTO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "clave"))
                .andExpect(content().json("{\"number\":\"5491122334455\",\"text\":\"hola\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        cliente = new ClienteEvolutionApi(builder.build(), "ensambla", "5491100000000");
    }

    private void dadoUnEvolutionQueResponde(String ruta, ResponseCreator respuesta) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + ruta)).andRespond(respuesta);
        cliente = new ClienteEvolutionApi(builder.build(), "ensambla", "");
    }

    private void dadoUnEvolutionQueEntregaUnaFotoPng() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + DESCARGAR_MEDIA))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"message\":{\"key\":{\"id\":\"F1\"}},\"convertToMp4\":false}"))
                .andRespond(withSuccess("{\"base64\":\"/9j/AAAA\",\"mimetype\":\"image/png\"}",
                        MediaType.APPLICATION_JSON));
        cliente = new ClienteEvolutionApi(builder.build(), "ensambla", "");
    }

    private void dadoUnClienteSimuladoQueFallaLosEnvios() {
        simulada = new MensajeriaWhatsappSimulada("");
        simulada.fallarEnvios(true);
    }

    private void dadoUnClienteSimuladoConUnaFotoCargada() {
        simulada = new MensajeriaWhatsappSimulada("");
        propia = new ArchivoWhatsapp(new byte[]{1}, "image/png");
        simulada.ponerMedia("F1", propia);
    }

    private void dadoUnClienteSimuladoSinFotos() {
        simulada = new MensajeriaWhatsappSimulada("");
    }

    private void dadoUnClienteEvolutionConElNumeroDelBot() {
        cliente = new ClienteEvolutionApi(RestClient.create(BASE), "ensambla", "5491100000000");
    }

    private void dadoUnClienteSimuladoConElNumeroDelBot() {
        simulada = new MensajeriaWhatsappSimulada("5491100000000");
    }

    private void dadoLaConfiguracionDeWhatsapp() {
        properties = new WhatsappProperties("evolution", "5491100000000", "secreto",
                new WhatsappProperties.Evolution("http://localhost:8081", "clave", "ensambla", Duration.ofSeconds(5)),
                new WhatsappProperties.Atencion(false, Duration.ofSeconds(1), Duration.ofMinutes(2), 0.6));
        config = new WhatsappConfig();
    }

    // --- cuando ---

    private void cuandoSeEnvia(String texto) {
        entregado = cliente.enviarTexto("5491122334455", texto);
    }

    private void cuandoSeDescargaLaMedia() {
        archivo = cliente.descargarMedia("F1");
    }

    private void cuandoSeIntentaDescargarLaMedia() {
        fallo = catchThrowable(this::cuandoSeDescargaLaMedia);
    }

    private void cuandoSeEnviaConElClienteSimulado() {
        entregado = simulada.enviarTexto("5491122334455", "hola");
    }

    private void cuandoSeDescargaLaFotoCargadaDelSimulado() {
        archivo = simulada.descargarMedia("F1");
    }

    private void cuandoSeDescargaUnaFotoNoCargadaDelSimulado() {
        archivo = simulada.descargarMedia("otra");
    }

    // --- entonces ---

    private void entoncesEvolutionRecibioLoEsperado() {
        server.verify();
    }

    private void entoncesSeEntrego() {
        assertThat(entregado).isTrue();
    }

    private void entoncesNoSeEntrego() {
        assertThat(entregado).isFalse();
    }

    private void entoncesSeDecodificaLaFotoPng() {
        assertThat(archivo.contenido()).isEqualTo(Base64.getDecoder().decode("/9j/AAAA"));
        assertThat(archivo.mime()).isEqualTo("image/png");
        server.verify();
    }

    private void entoncesElTipoEs(String esperado) {
        assertThat(archivo.mime()).isEqualTo(esperado);
    }

    private void entoncesFallaPorqueNoHayArchivo() {
        assertThat(fallo).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesElEnvioSimuladoFalloYNoQuedoRegistrado() {
        assertThat(entregado).isFalse();
        assertThat(simulada.enviados()).isEmpty();
    }

    private void entoncesSeBajaLaFotoCargada() {
        assertThat(archivo).isSameAs(propia);
    }

    private void entoncesSeBajaUnaFotoDeEjemploJpeg() {
        assertThat(archivo.mime()).isEqualTo("image/jpeg");
        assertThat(archivo.contenido()).isNotEmpty();
    }

    private void entoncesElNumeroDelBotEs(String esperado) {
        assertThat(cliente.numeroBot()).isEqualTo(esperado);
    }

    private void entoncesElSimuladoGuardoElEnvioYInformaElNumeroDelBot() {
        assertThat(simulada.enviados())
                .containsExactly(new MensajeriaWhatsappSimulada.Enviado("5491122334455", "hola"));
        assertThat(simulada.numeroBot()).isEqualTo("5491100000000");
    }

    private void entoncesSeArmanAmbosProveedores() {
        assertThat(config.mensajeriaWhatsappSimulada(properties)).isInstanceOf(MensajeriaWhatsappSimulada.class);
        assertThat(config.mensajeriaWhatsappEvolution(properties)).isInstanceOf(ClienteEvolutionApi.class);
        assertThat(config.evolutionWebhookParser(Clock.systemUTC())).isNotNull();
    }
}
