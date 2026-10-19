package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Cableado de Sami: el diccionario de {@code ia/sami-diccionario.json} (si está mal formado no levanta) y el
 * respaldo con IA. Para activar Bedrock: {@code ENSAMBLA_SAMI_IA_PROVEEDOR=bedrock}, la región y las credenciales
 * de AWS (perfil o variables de entorno). Ver {@link SamiProperties.Ia}.
 */
@Configuration
@EnableConfigurationProperties(SamiProperties.class)
public class SamiConfig {

    @Bean
    DiccionarioSami diccionarioSami(ObjectMapper objectMapper) {
        String json = RecursoTexto.contenido(new ClassPathResource("ia/sami-diccionario.json"));
        return objectMapper.readValue(json, DiccionarioSami.class);
    }

    @Bean
    PromptSami promptSami() {
        return new PromptSami(
                new ClassPathResource("ia/prompt-sami.md"), new ClassPathResource("ia/esquema-sami.json"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.sami.ia", name = "proveedor", havingValue = "bedrock")
    BedrockRuntimeClient bedrockRuntimeClient(SamiProperties properties) {
        SamiProperties.Ia ia = properties.ia();
        AwsCredentialsProvider credenciales = ia.perfilAws() == null || ia.perfilAws().isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : ProfileCredentialsProvider.create(ia.perfilAws());
        return BedrockRuntimeClient.builder()
                .region(Region.of(ia.region()))
                .credentialsProvider(credenciales)
                .overrideConfiguration(config -> config.apiCallTimeout(ia.timeout()))
                .build();
    }

    @Bean
    SamiIaPort samiIaPort(PromptSami promptSami, ObjectMapper objectMapper, SamiProperties properties,
                          ObjectProvider<BedrockRuntimeClient> bedrock) {
        SamiProperties.Ia ia = properties.ia();
        List<ClienteModeloTexto> modelos = new ArrayList<>();
        BedrockRuntimeClient cliente = bedrock.getIfAvailable();
        if (cliente == null) {
            modelos.add(new ClienteModeloTextoSimulado());
        } else {
            modelos.add(new ClienteModeloTextoBedrock(cliente, ia.modeloRapido(), ia.maxTokens()));
            if (ia.modeloAvanzado() != null && !ia.modeloAvanzado().isBlank()) {
                modelos.add(new ClienteModeloTextoBedrock(cliente, ia.modeloAvanzado(), ia.maxTokens()));
            }
        }
        return new SamiIaAdapter(modelos, promptSami, objectMapper, ia.umbralEscalar());
    }

    @Bean
    PromptTaller promptTaller() {
        return new PromptTaller(new ClassPathResource("ia/prompt-taller.md"));
    }

    /**
     * IA del taller conversacional: una sola llamada por turno al modelo rápido, con las herramientas de
     * {@code ia/herramientas-taller.json}. Sin Bedrock responde el cliente simulado, que no cuesta nada.
     */
    @Bean
    TallerIaPort tallerIaPort(PromptTaller promptTaller, ObjectMapper objectMapper, SamiProperties properties,
                              ObjectProvider<BedrockRuntimeClient> bedrock) {
        SamiProperties.Ia ia = properties.ia();
        List<DefinicionHerramienta> herramientas = List.of(objectMapper.readValue(
                RecursoTexto.contenido(new ClassPathResource("ia/herramientas-taller.json")),
                DefinicionHerramienta[].class));
        BedrockRuntimeClient cliente = bedrock.getIfAvailable();
        ClienteModeloConHerramientas modelo = cliente == null
                ? new ClienteModeloConHerramientasSimulado()
                : new ClienteModeloConHerramientasBedrock(
                        cliente, ia.modeloRapido(), ia.maxTokensTaller(), herramientas);
        return new TallerIaAdapter(modelo, promptTaller);
    }
}
