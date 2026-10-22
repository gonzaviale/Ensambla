package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SamiPropertiesTest {

    private static final Duration TIMEOUT_SAMI = Duration.ofSeconds(30);
    private static final IaProperties GLOBAL = new IaProperties("anthropic", "vision-pisada", Duration.ofSeconds(60),
            10, 0.8, Map.of(
                    "anthropic", new IaProperties.Proveedor(
                            "clave", null, null, "claude-haiku-4-5", "claude-sonnet-4-6", "claude-haiku-4-5"),
                    "bedrock", new IaProperties.Proveedor(
                            null, "us-east-1", "perfil-global", "haiku-bedrock", "sonnet-bedrock", "vision-bedrock")));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin proveedor propio, Sami usa el global con sus credenciales y modelos")
    void ajustesShouldUseGlobalProviderWhenSamiDoesNotOverride(String proveedor) {
        AjustesIa ajustes = ia(proveedor, null, null, null, null).ajustes(GLOBAL);

        assertThat(ajustes).isEqualTo(new AjustesIa("anthropic", "clave", null, null, "claude-haiku-4-5",
                "claude-sonnet-4-6", "claude-haiku-4-5", TIMEOUT_SAMI));
    }

    @Test
    @DisplayName("Si Sami pisa el proveedor, usa ése con las credenciales y los modelos de ése")
    void ajustesShouldUseSamiProviderWhenOverridden() {
        AjustesIa ajustes = ia(" Bedrock", null, null, null, null).ajustes(GLOBAL);

        assertThat(ajustes).isEqualTo(new AjustesIa("bedrock", null, "us-east-1", "perfil-global", "haiku-bedrock",
                "sonnet-bedrock", "vision-bedrock", TIMEOUT_SAMI));
    }

    @Test
    @DisplayName("La región, el perfil y los modelos de Sami pisan los del proveedor")
    void ajustesShouldOverrideRegionProfileAndModels() {
        AjustesIa ajustes = ia("bedrock", "sa-east-1", "perfil-sami", "rapido-sami", "avanzado-sami").ajustes(GLOBAL);

        assertThat(ajustes.region()).isEqualTo("sa-east-1");
        assertThat(ajustes.perfilAws()).isEqualTo("perfil-sami");
        assertThat(ajustes.modeloRapido()).isEqualTo("rapido-sami");
        assertThat(ajustes.modeloAvanzado()).isEqualTo("avanzado-sami");
    }

    // --- helpers ---
    private SamiProperties.Ia ia(String proveedor, String region, String perfil, String rapido, String avanzado) {
        return new SamiProperties.Ia(proveedor, region, perfil, rapido, avanzado, TIMEOUT_SAMI, 700, 0.6, 900);
    }
}
