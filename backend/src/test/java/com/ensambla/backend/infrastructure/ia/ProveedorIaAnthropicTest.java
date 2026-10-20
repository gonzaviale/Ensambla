package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProveedorIaAnthropicTest {

    private static final AjustesIa AJUSTES = ajustes("clave-de-prueba");
    private static final List<DefinicionHerramienta> HERRAMIENTAS = List.of(new DefinicionHerramienta(
            "renombrar_sistema", "Renombra el sistema.", Map.of("type", "object", "properties", Map.of())));

    private final List<AjustesIa> conexiones = new ArrayList<>();
    private AnthropicClient cliente;
    private ProveedorIaAnthropic proveedor;

    @BeforeEach
    void setUp() {
        cliente = mock(AnthropicClient.class);
        proveedor = new ProveedorIaAnthropic(ajustes -> {
            conexiones.add(ajustes);
            return cliente;
        });
    }

    @Test
    @DisplayName("Arma las tres puertas con los clientes de la API de Anthropic, con un solo cliente por clave")
    void proveedorShouldBuildTheThreeAnthropicClients() {
        ClienteModeloVision vision = proveedor.vision(AJUSTES);
        ClienteModeloTexto texto = proveedor.texto(AJUSTES, "claude-sonnet-4-6", 700);
        ClienteModeloConHerramientas conHerramientas = proveedor.conHerramientas(AJUSTES, 900, HERRAMIENTAS);

        assertThat(proveedor.nombre()).isEqualTo("anthropic");
        assertThat(vision).isInstanceOf(ClienteModeloVisionAnthropic.class);
        assertThat(texto).isInstanceOf(ClienteModeloTextoAnthropic.class);
        assertThat(conHerramientas).isInstanceOf(ClienteModeloConHerramientasAnthropic.class);
        assertThat(conexiones).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin la clave no arranca y dice qué variable falta")
    void proveedorShouldFailWithoutApiKey(String clave) {
        assertThatThrownBy(() -> proveedor.vision(ajustes(clave)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ProveedorIaAnthropic.FALTA_CLAVE);
    }

    @Test
    @DisplayName("Al cerrar cierra los clientes que abrió")
    void closeShouldCloseOpenClients() {
        proveedor.texto(AJUSTES, "claude-haiku-4-5", 700);

        proveedor.close();

        verify(cliente).close();
    }

    @Test
    @DisplayName("Sin la función de prueba, conecta de verdad (sin llamar todavía a la red)")
    void defaultConstructorShouldBuildRealClients() {
        try (ProveedorIaAnthropic real = new ProveedorIaAnthropic()) {
            assertThat(real.texto(AJUSTES, "claude-haiku-4-5", 700)).isInstanceOf(ClienteModeloTextoAnthropic.class);
        }
    }

    // --- helpers ---
    private static AjustesIa ajustes(String clave) {
        return new AjustesIa("anthropic", clave, null, null, "claude-haiku-4-5", "claude-sonnet-4-6",
                "claude-haiku-4-5", Duration.ofSeconds(30));
    }
}
