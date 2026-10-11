package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/**
 * Cableado de la lectura con IA. Para conectar un proveedor real:
 * <ol>
 *   <li>implementar {@link ClienteModeloVision} (por ejemplo {@code ClienteModeloVisionAnthropic});</li>
 *   <li>registrarlo acá con {@code @ConditionalOnProperty(prefix = "ensambla.ia", name = "proveedor",
 *       havingValue = "<nombre>")};</li>
 *   <li>configurar {@code ENSAMBLA_IA_PROVEEDOR}, {@code ENSAMBLA_IA_MODELO} y {@code ENSAMBLA_IA_API_KEY}.</li>
 * </ol>
 * El resto (prompt, esquema, validación y consumo) ya lo resuelve {@link LectorCuadernoIaAdapter}.
 */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.ia", name = "proveedor", havingValue = "simulado", matchIfMissing = true)
    ClienteModeloVision clienteModeloVisionSimulado() {
        return new ClienteModeloVisionSimulado(new ClassPathResource("ia/lectura-simulada.json"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.ia", name = "proveedor", havingValue = "anthropic")
    ClienteModeloVision clienteModeloVisionAnthropic(IaProperties properties) {
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(properties.timeout())
                .build();
        return new ClienteModeloVisionAnthropic(client, properties.modelo());
    }

    @Bean
    PromptLecturaCuaderno promptLecturaCuaderno() {
        return new PromptLecturaCuaderno(new ClassPathResource("ia/prompt-lectura-cuaderno.md"),
                new ClassPathResource("ia/esquema-lectura-cuaderno.json"));
    }

    @Bean
    LectorCuadernoPort lectorCuaderno(ClienteModeloVision cliente, PromptLecturaCuaderno prompt,
                                      ObjectMapper objectMapper, IaProperties properties) {
        return new LectorCuadernoIaAdapter(cliente, prompt, objectMapper, properties.maxPaginasPorLectura());
    }
}
