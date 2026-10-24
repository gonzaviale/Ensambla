package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba manual contra Amazon Bedrock de verdad: cuesta centavos. Se salta sola, salvo que se defina
 * {@code SAMI_BEDROCK_PERFIL} con el nombre de un perfil de {@code ~/.aws}. Opcionales: {@code SAMI_BEDROCK_REGION}
 * (por defecto us-east-1), {@code SAMI_BEDROCK_RAPIDO} y {@code SAMI_BEDROCK_AVANZADO}.
 * <pre>SAMI_BEDROCK_PERFIL=mi-perfil ./mvnw test -Dtest=SamiBedrockRealTest</pre>
 */
@EnabledIfEnvironmentVariable(named = "SAMI_BEDROCK_PERFIL", matches = ".+")
class SamiBedrockRealTest {

    private static final String RAPIDO = System.getenv().getOrDefault(
            "SAMI_BEDROCK_RAPIDO", "us.anthropic.claude-haiku-4-5-20251001-v1:0");
    private static final String AVANZADO = System.getenv().getOrDefault(
            "SAMI_BEDROCK_AVANZADO", "us.anthropic.claude-sonnet-4-6");

    @Test
    @DisplayName("Con Bedrock real, Sami elige una plantilla existente y registra los tokens")
    void proponerShouldReturnExistingTemplateWithRealBedrock() {
        try (BedrockRuntimeClient bedrock = BedrockRuntimeClient.builder()
                .region(Region.of(System.getenv().getOrDefault("SAMI_BEDROCK_REGION", "us-east-1")))
                .credentialsProvider(ProfileCredentialsProvider.create(System.getenv("SAMI_BEDROCK_PERFIL")))
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(60)))
                .build()) {
            SamiIaAdapter adapter = new SamiIaAdapter(
                    List.of(new ClienteModeloTextoBedrock(bedrock, RAPIDO, 700),
                            new ClienteModeloTextoBedrock(bedrock, AVANZADO, 700)),
                    new PromptSami(new org.springframework.core.io.ClassPathResource("ia/prompt-sami.md"),
                            new org.springframework.core.io.ClassPathResource("ia/esquema-sami.json")),
                    JsonMapper.builder().build(),
                    0.6);

            for (String texto : List.of(
                    "Vendo velas y anoto quién me debe",
                    "hago tortas por encargo y anoto la seña",
                    "tengo un taller mecánico y me cuesta ordenar los repuestos, los autos y lo que cobro")) {
                PropuestaIaSami propuesta = adapter.proponer(
                        new ConsultaIaSami(texto, List.of(), SamiPruebas.plantillas()));

                System.out.printf("SAMI-BEDROCK «%s» → %s %s confianza=%.2f modelo=%s tokens=%d/%d%n", texto,
                        propuesta.plantilla(), propuesta.columnasExtra(), propuesta.confianza(),
                        propuesta.consumo().modelo(), propuesta.consumo().tokensEntrada(),
                        propuesta.consumo().tokensSalida());
                assertThat(propuesta.aInterpretacion(SamiPruebas.plantillas())).isPresent();
                assertThat(propuesta.consumo().tokensEntrada()).isPositive();
            }
        }
    }
}
