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
    void sinConfigurarNadaLaLecturaYSamiUsanElProveedorSimulado() {
        contexto.run(context -> {
            entoncesLaLecturaEsSimulada(context.getBean(ClienteModeloVision.class));
            entoncesSamiConversaConElProveedor(context.getBean(TallerIaPort.class), "simulado");
        });
    }

    @Test
    @DisplayName("Con bedrock global, la lectura lee con Bedrock y Sami puede pisarlo con el simulado")
    void conBedrockGlobalLaLecturaLeeConBedrockYSamiPuedePisarloConElSimulado() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedor=bedrock",
                        "ensambla.ia.proveedores.bedrock.region=us-east-1",
                        "ensambla.ia.proveedores.bedrock.perfil-aws=perfil-de-prueba",
                        "ensambla.ia.proveedores.bedrock.modelo-vision=vision-bedrock",
                        "ensambla.sami.ia.proveedor=simulado")
                .run(context -> {
                    entoncesLaLecturaEsDeBedrock(context.getBean(ClienteModeloVision.class));
                    entoncesElModeloDeVisionEs(context.getBean(IaProperties.class), "vision-bedrock");
                    entoncesSamiConversaConElProveedor(context.getBean(TallerIaPort.class), "simulado");
                });
    }

    @Test
    @DisplayName("Con anthropic global y su clave, la lectura usa la API de Anthropic con el modelo que pisa")
    void conAnthropicGlobalYSuClaveLaLecturaUsaLaApiDeAnthropicConElModeloQuePisa() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedor=anthropic",
                        "ensambla.ia.modelo=claude-opus",
                        "ensambla.ia.proveedores.anthropic.api-key=clave-de-prueba",
                        "ensambla.ia.proveedores.anthropic.modelo-rapido=claude-haiku-4-5",
                        "ensambla.ia.proveedores.anthropic.modelo-vision=claude-haiku-4-5",
                        "ensambla.sami.ia.proveedor=simulado")
                .run(context -> {
                    entoncesLaLecturaEsDeAnthropic(context.getBean(ClienteModeloVision.class));
                    entoncesElModeloDeVisionEs(context.getBean(IaProperties.class), "claude-opus");
                });
    }

    @Test
    @DisplayName("Si Sami pisa el proveedor, sus ajustes salen del proveedor de Sami con lo que pisa")
    void siSamiPisaElProveedorSusAjustesSalenDelProveedorDeSamiConLoQuePisa() {
        contexto.withPropertyValues(
                        "ensambla.ia.proveedores.bedrock.region=us-east-1",
                        "ensambla.ia.proveedores.bedrock.modelo-rapido=haiku-bedrock",
                        "ensambla.sami.ia.proveedor=bedrock",
                        "ensambla.sami.ia.perfil-aws=perfil-sami")
                .run(context -> entoncesLosAjustesDeSamiSalenDeBedrockConSuPerfil(
                        context.getBean(SamiProperties.class).ia().ajustes(context.getBean(IaProperties.class))));
    }

    @Test
    @DisplayName("Un proveedor desconocido no deja levantar la aplicación")
    void unProveedorDesconocidoNoDejaLevantarLaAplicacion() {
        contexto.withPropertyValues("ensambla.ia.proveedor=openai")
                .run(context -> assertThat(context).hasFailed());
    }

    // --- entonces ---

    private void entoncesLaLecturaEsSimulada(ClienteModeloVision vision) {
        assertThat(vision).isInstanceOf(ClienteModeloVisionSimulado.class);
    }

    private void entoncesLaLecturaEsDeBedrock(ClienteModeloVision vision) {
        assertThat(vision).isInstanceOf(ClienteModeloVisionBedrock.class);
    }

    private void entoncesLaLecturaEsDeAnthropic(ClienteModeloVision vision) {
        assertThat(vision).isInstanceOf(ClienteModeloVisionAnthropic.class);
    }

    private void entoncesElModeloDeVisionEs(IaProperties properties, String esperado) {
        assertThat(properties.modeloVision()).isEqualTo(esperado);
    }

    private void entoncesSamiConversaConElProveedor(TallerIaPort taller, String esperado) {
        String proveedor = taller.conversar(new ConsultaTaller("hola",
                new EstadoTaller("Mi sistema", "mosaic", List.of()), List.of())).consumo().proveedor();
        assertThat(proveedor).isEqualTo(esperado);
    }

    private void entoncesLosAjustesDeSamiSalenDeBedrockConSuPerfil(AjustesIa ajustes) {
        assertThat(ajustes.proveedor()).isEqualTo("bedrock");
        assertThat(ajustes.region()).isEqualTo("us-east-1");
        assertThat(ajustes.perfilAws()).isEqualTo("perfil-sami");
        assertThat(ajustes.modeloRapido()).isEqualTo("haiku-bedrock");
    }
}
