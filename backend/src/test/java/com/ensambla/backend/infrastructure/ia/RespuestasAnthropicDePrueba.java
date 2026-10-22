package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolUseBlock;
import org.mockito.Answers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Clientes de la API de Anthropic simulados con Mockito que contestan siempre lo mismo. */
final class RespuestasAnthropicDePrueba {

    static final long TOKENS_ENTRADA = 10;
    static final long TOKENS_SALIDA = 5;

    private RespuestasAnthropicDePrueba() {
    }

    /** Un cliente cuyo modelo contesta los bloques indicados. */
    static AnthropicClient anthropicQueResponde(ContentBlock... bloques) {
        AnthropicClient anthropic = mock(AnthropicClient.class, Answers.RETURNS_DEEP_STUBS);
        Message mensaje = mock(Message.class, Answers.RETURNS_DEEP_STUBS);
        when(mensaje.content()).thenReturn(List.of(bloques));
        when(mensaje.usage().inputTokens()).thenReturn(TOKENS_ENTRADA);
        when(mensaje.usage().outputTokens()).thenReturn(TOKENS_SALIDA);
        when(anthropic.messages().create(any(MessageCreateParams.class))).thenReturn(mensaje);
        return anthropic;
    }

    static AnthropicClient anthropicQueResponde(String texto) {
        return anthropicQueResponde(texto(texto));
    }

    static ContentBlock texto(String texto) {
        ContentBlock bloque = mock(ContentBlock.class);
        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn(texto);
        when(bloque.text()).thenReturn(Optional.of(textBlock));
        when(bloque.toolUse()).thenReturn(Optional.empty());
        return bloque;
    }

    /** Un bloque {@code tool_use} con esos argumentos; {@code null} simula una entrada que no es un objeto. */
    static ContentBlock herramienta(String nombre, Map<String, Object> argumentos) {
        ContentBlock bloque = mock(ContentBlock.class);
        ToolUseBlock uso = mock(ToolUseBlock.class);
        when(uso.name()).thenReturn(nombre);
        when(uso._input()).thenReturn(JsonValue.from(argumentos));
        when(bloque.text()).thenReturn(Optional.empty());
        when(bloque.toolUse()).thenReturn(Optional.of(uso));
        return bloque;
    }
}
