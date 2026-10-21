package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IaPropertiesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final IaProperties.Proveedor ANTHROPIC = new IaProperties.Proveedor(
            "clave", null, null, "claude-haiku-4-5", "claude-sonnet-4-6", "claude-haiku-4-5");
    private static final IaProperties.Proveedor BEDROCK = new IaProperties.Proveedor(
            null, "us-west-2", "perfil", "haiku-bedrock", "sonnet-bedrock", "vision-bedrock");
    private static final Map<String, IaProperties.Proveedor> PROVEEDORES =
            Map.of("anthropic", ANTHROPIC, "bedrock", BEDROCK);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin proveedor, el global es el simulado")
    void proveedorShouldDefaultToSimulatedWhenBlank(String proveedor) {
        IaProperties properties = properties(proveedor, null);

        assertThat(properties.proveedor()).isEqualTo("simulado");
    }

    @Test
    @DisplayName("El nombre del proveedor no depende de mayúsculas ni espacios")
    void proveedorShouldBeNormalized() {
        IaProperties properties = properties(" Bedrock ", null);

        assertThat(properties.proveedor()).isEqualTo("bedrock");
    }

    @Test
    @DisplayName("Los ajustes globales salen de las credenciales y los modelos del proveedor elegido")
    void ajustesShouldUseTheChosenProviderSettings() {
        AjustesIa ajustes = properties("bedrock", null).ajustes();

        assertThat(ajustes).isEqualTo(new AjustesIa("bedrock", null, "us-west-2", "perfil", "haiku-bedrock",
                "sonnet-bedrock", "vision-bedrock", TIMEOUT));
    }

    @Test
    @DisplayName("ENSAMBLA_IA_MODELO pisa el modelo de visión del proveedor")
    void ajustesShouldOverrideVisionModelWithModelo() {
        IaProperties properties = properties("anthropic", "claude-opus");

        assertThat(properties.ajustes().modeloVision()).isEqualTo("claude-opus");
        assertThat(properties.ajustes().apiKey()).isEqualTo("clave");
        assertThat(properties.modeloVision()).isEqualTo("claude-opus");
    }

    @Test
    @DisplayName("El simulado no tiene credenciales ni modelos")
    void ajustesShouldBeEmptyForSimulatedProvider() {
        AjustesIa ajustes = properties("simulado", null).ajustes();

        assertThat(ajustes).isEqualTo(new AjustesIa("simulado", null, null, null, null, null, null, TIMEOUT));
    }

    @Test
    @DisplayName("Sin proveedores configurados, cualquiera queda sin datos")
    void deShouldReturnEmptySettingsWithoutProviders() {
        IaProperties properties = new IaProperties("bedrock", null, TIMEOUT, 10, 0.8, null);

        assertThat(properties.de("bedrock")).isEqualTo(new IaProperties.Proveedor(null, null, null, null, null, null));
        assertThat(properties.modeloVision()).isNull();
    }

    @Test
    @DisplayName("Un modelo de visión en blanco cuenta como que no hay ninguno")
    void modeloVisionShouldBeNullWhenBlank() {
        IaProperties properties = new IaProperties("bedrock", " ", TIMEOUT, 10, 0.8,
                Map.of("bedrock", new IaProperties.Proveedor(null, "us-east-1", null, "rapido", null, "")));

        assertThat(properties.modeloVision()).isNull();
    }

    // --- helpers ---
    private IaProperties properties(String proveedor, String modelo) {
        return new IaProperties(proveedor, modelo, TIMEOUT, 10, 0.8, PROVEEDORES);
    }
}
