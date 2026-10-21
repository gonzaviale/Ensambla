package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.port.TallerIaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** La configuración completa, como la levanta Spring: qué puerta se arma según las propiedades. */
class ProveedorIaConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(IaConfig.class, SamiConfig.class)
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());

    @Test
    @DisplayName("Sin configurar nada, la lectura y Sami usan el proveedor simulado")
    void contextShouldUseSimulatedProviderByDefault() {
        contexto.run(context -> {
            assertThat(context.getBean(ClienteModeloVision.class)).isInstanceOf(ClienteModeloVisionSimulado.class);
            assertThat(conversar(context.getBean(TallerIaPort.class))).isEqualTo("simulado");
        });
    }

    @Test
    @DisplayName("Con bedrock global, la lectura lee con Bedrock y Sami puede pisarlo con el simulado")
    void contextShouldUseBedrockForVisionAndSamiOverride() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedor=bedrock",
                        "ensambla.ia.proveedores.bedrock.region=us-east-1",
                        "ensambla.ia.proveedores.bedrock.perfil-aws=perfil-de-prueba",
                        "ensambla.ia.proveedores.bedrock.modelo-vision=vision-bedrock",
                        "ensambla.sami.ia.proveedor=simulado")
                .run(context -> {
                    assertThat(context.getBean(ClienteModeloVision.class))
                            .isInstanceOf(ClienteModeloVisionBedrock.class);
                    assertThat(context.getBean(IaProperties.class).modeloVision()).isEqualTo("vision-bedrock");
                    assertThat(conversar(context.getBean(TallerIaPort.class))).isEqualTo("simulado");
                });
    }

    @Test
    @DisplayName("Con anthropic global y su clave, la lectura usa la API de Anthropic con el modelo que pisa")
    void contextShouldUseAnthropicForVision() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedor=anthropic",
                        "ensambla.ia.modelo=claude-opus",
                        "ensambla.ia.proveedores.anthropic.api-key=clave-de-prueba",
                        "ensambla.ia.proveedores.anthropic.modelo-rapido=claude-haiku-4-5",
                        "ensambla.ia.proveedores.anthropic.modelo-vision=claude-haiku-4-5",
                        "ensambla.sami.ia.proveedor=simulado")
                .run(context -> {
                    assertThat(context.getBean(ClienteModeloVision.class))
                            .isInstanceOf(ClienteModeloVisionAnthropic.class);
                    assertThat(context.getBean(IaProperties.class).modeloVision()).isEqualTo("claude-opus");
                });
    }

    @Test
    @DisplayName("Si Sami pisa el proveedor, sus ajustes salen del proveedor de Sami con lo que pisa")
    void contextShouldBindSamiOverrides() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedores.bedrock.region=us-east-1",
                        "ensambla.ia.proveedores.bedrock.modelo-rapido=haiku-bedrock",
                        "ensambla.sami.ia.proveedor=bedrock",
                        "ensambla.sami.ia.perfil-aws=perfil-sami")
                .run(context -> {
                    AjustesIa ajustes = context.getBean(SamiProperties.class).ia()
                            .ajustes(context.getBean(IaProperties.class));
                    assertThat(ajustes.proveedor()).isEqualTo("bedrock");
                    assertThat(ajustes.region()).isEqualTo("us-east-1");
                    assertThat(ajustes.perfilAws()).isEqualTo("perfil-sami");
                    assertThat(ajustes.modeloRapido()).isEqualTo("haiku-bedrock");
                });
    }

    @Test
    @DisplayName("Un proveedor desconocido no deja levantar la aplicación")
    void contextShouldFailWithUnknownProvider() {
        contexto.withPropertyValues("ensambla.ia.proveedor=openai")
                .run(context -> assertThat(context).hasFailed());
    }

    // --- act ---
    private String conversar(TallerIaPort taller) {
        return taller.conversar(new ConsultaTaller("hola", new EstadoTaller("Mi sistema", "mosaic", List.of()),
                List.of())).consumo().proveedor();
    }
}
