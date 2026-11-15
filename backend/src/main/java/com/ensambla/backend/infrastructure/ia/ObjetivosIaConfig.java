package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/**
 * Cableado de la IA que redacta los mini-objetivos. Usa el proveedor y el modelo rápido de Sami
 * ({@link SamiProperties.Ia#ajustes(IaProperties)}); si la redacción está apagada
 * ({@code ensambla.objetivos.ia.habilitada}) nunca se consulta.
 */
@Configuration
@EnableConfigurationProperties(ObjetivosIaProperties.class)
public class ObjetivosIaConfig {

    @Bean
    PromptObjetivos promptObjetivos() {
        return new PromptObjetivos(
                new ClassPathResource("ia/prompt-objetivos.md"), new ClassPathResource("ia/esquema-objetivos.json"));
    }

    @Bean
    PromptConversacionObjetivos promptConversacionObjetivos() {
        return new PromptConversacionObjetivos(
                new ClassPathResource("ia/prompt-conversacion-objetivos.md"),
                new ClassPathResource("ia/esquema-conversacion-objetivos.json"));
    }

    /** Conversa los objetivos con el modelo rápido de Sami; con el proveedor simulado no tiene costo. */
    @Bean
    ConversacionObjetivosIaPort conversacionObjetivosIaPort(PromptConversacionObjetivos prompt,
                                                           ObjectMapper objectMapper, SamiProperties properties,
                                                           IaProperties iaProperties, ProveedoresIa proveedores) {
        SamiProperties.Ia ia = properties.ia();
        AjustesIa ajustes = ia.ajustes(iaProperties);
        ClienteModeloTexto modelo = proveedores.elegir(ajustes.proveedor())
                .texto(ajustes, ajustes.modeloRapido(), ia.maxTokensTaller());
        return new ConversacionObjetivosIaAdapter(modelo, prompt, objectMapper);
    }

    @Bean
    RedaccionObjetivosIaPort redaccionObjetivosIaPort(PromptObjetivos prompt, ObjectMapper objectMapper,
                                                      SamiProperties properties, IaProperties iaProperties,
                                                      ProveedoresIa proveedores) {
        SamiProperties.Ia ia = properties.ia();
        AjustesIa ajustes = ia.ajustes(iaProperties);
        ClienteModeloTexto modelo = proveedores.elegir(ajustes.proveedor())
                .texto(ajustes, ajustes.modeloRapido(), ia.maxTokens());
        return new RedaccionObjetivosIaAdapter(modelo, prompt, objectMapper);
    }
}
