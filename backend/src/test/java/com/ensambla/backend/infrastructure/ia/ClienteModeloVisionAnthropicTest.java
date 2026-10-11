package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteModeloVisionAnthropicTest {

    private static final String MODELO = "claude-haiku-5-5";
    private static final PeticionModeloVision PETICION = new PeticionModeloVision("Leé el cuaderno", "Bloques: ninguno",
            List.of(new PaginaCuaderno(1, new byte[]{1, 2}, "image/jpeg"),
                    new PaginaCuaderno(2, new byte[]{3}, "image/png")), "{\"type\":\"object\"}");

    private AnthropicClient client;
    private ClienteModeloVisionAnthropic cliente;

    @BeforeEach
    void setUp() {
        client = mock(AnthropicClient.class, Answers.RETURNS_DEEP_STUBS);
        cliente = new ClienteModeloVisionAnthropic(client, MODELO);
    }

    @Test
    @DisplayName("Devuelve el JSON de la respuesta con los tokens y el modelo informados")
    void completarShouldReturnJsonWithTokensWhenModelRespondsPlainJson() {
        givenModelResponds("{\"a\":1}", 120, 30);

        RespuestaModeloVision respuesta = cliente.completar(PETICION);

        thenRespuestaIs(respuesta, "{\"a\":1}", 120, 30);
    }

    @Test
    @DisplayName("Descarta la cerca de código que el modelo ponga alrededor del JSON")
    void completarShouldStripCodeFenceWhenModelWrapsJson() {
        givenModelResponds("```json\n{\"a\":1}\n```", 1, 2);

        RespuestaModeloVision respuesta = cliente.completar(PETICION);

        thenRespuestaIs(respuesta, "{\"a\":1}", 1, 2);
    }

    @Test
    @DisplayName("Si la respuesta no trae ningún objeto JSON la devuelve tal cual para que falle la validación")
    void completarShouldReturnTextAsIsWhenThereIsNoJsonObject() {
        givenModelResponds("no puedo leer la foto", 1, 2);

        RespuestaModeloVision respuesta = cliente.completar(PETICION);

        thenRespuestaIs(respuesta, "no puedo leer la foto", 1, 2);
    }

    @Test
    @DisplayName("Si el JSON viene cortado, sin llave de cierre, lo devuelve tal cual para que falle la validación")
    void completarShouldReturnTextAsIsWhenJsonIsTruncated() {
        givenModelResponds("{\"a\":", 1, 2);

        RespuestaModeloVision respuesta = cliente.completar(PETICION);

        thenRespuestaIs(respuesta, "{\"a\":", 1, 2);
    }

    @Test
    @DisplayName("Manda el modelo, las instrucciones con el esquema, el contexto y cada página como imagen")
    void completarShouldSendModelInstructionsContextAndPages() {
        givenModelResponds("{}", 0, 0);

        cliente.completar(PETICION);

        thenRequestCarriesEverything();
    }

    // --- arrange ---
    private void givenModelResponds(String texto, long entrada, long salida) {
        Message mensaje = mock(Message.class, Answers.RETURNS_DEEP_STUBS);
        ContentBlock bloqueTexto = mock(ContentBlock.class);
        TextBlock textBlock = mock(TextBlock.class);
        ContentBlock bloqueOtro = mock(ContentBlock.class);
        when(textBlock.text()).thenReturn(texto);
        when(bloqueTexto.text()).thenReturn(Optional.of(textBlock));
        when(bloqueOtro.text()).thenReturn(Optional.empty());
        when(mensaje.content()).thenReturn(List.of(bloqueOtro, bloqueTexto));
        when(mensaje.usage().inputTokens()).thenReturn(entrada);
        when(mensaje.usage().outputTokens()).thenReturn(salida);
        when(client.messages().create(any(MessageCreateParams.class))).thenReturn(mensaje);
    }

    // --- assert ---
    private void thenRespuestaIs(RespuestaModeloVision respuesta, String json, int entrada, int salida) {
        assertThat(respuesta.json()).isEqualTo(json);
        assertThat(respuesta.proveedor()).isEqualTo(ClienteModeloVisionAnthropic.PROVEEDOR);
        assertThat(respuesta.modelo()).isEqualTo(MODELO);
        assertThat(respuesta.tokensEntrada()).isEqualTo(entrada);
        assertThat(respuesta.tokensSalida()).isEqualTo(salida);
    }

    private void thenRequestCarriesEverything() {
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(client.messages()).create(captor.capture());
        String enviado = captor.getValue().toString();
        assertThat(enviado).contains(MODELO).contains("Leé el cuaderno").contains("{\"type\":\"object\"}")
                .contains("Bloques: ninguno").contains("Página 1:").contains("Página 2:")
                .contains("image/jpeg").contains("image/png").contains("AQI=");
    }
}
