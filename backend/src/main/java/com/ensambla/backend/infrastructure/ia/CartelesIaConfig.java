package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/**
 * Cableado de la IA que redacta los carteles. Por defecto es el proveedor simulado; con
 * {@code ensambla.carteles.ia.proveedor=real} usa el proveedor y el modelo rápido de Sami.
 */
@Configuration
@EnableConfigurationProperties(CartelesIaProperties.class)
public class CartelesIaConfig {

    @Bean
    @ConditionalOnProperty(name = "ensambla.carteles.ia.proveedor", havingValue = "simulado", matchIfMissing = true)
    RedaccionDeCartelIaPort redaccionDeCartelIaSimulada() {
        return new RedaccionDeCartelIaSimulada();
    }

    @Bean
    @ConditionalOnProperty(name = "ensambla.carteles.ia.proveedor", havingValue = "real")
    RedaccionDeCartelIaPort redaccionDeCartelIaReal(ObjectMapper objectMapper, SamiProperties properties,
                                                    IaProperties iaProperties, ProveedoresIa proveedores) {
        SamiProperties.Ia ia = properties.ia();
        AjustesIa ajustes = ia.ajustes(iaProperties);
        ClienteModeloTexto modelo = proveedores.elegir(ajustes.proveedor())
                .texto(ajustes, ajustes.modeloRapido(), ia.maxTokens());
        PromptCarteles prompt = new PromptCarteles(
                new ClassPathResource("ia/prompt-carteles.md"), new ClassPathResource("ia/esquema-carteles.json"));
        return new RedaccionDeCartelIaAdapter(modelo, prompt, objectMapper);
    }
}
