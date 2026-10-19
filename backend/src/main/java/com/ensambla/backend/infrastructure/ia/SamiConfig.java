package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/** Cableado de Sami: carga el diccionario de {@code ia/sami-diccionario.json}; si está mal formado no levanta. */
@Configuration
@EnableConfigurationProperties(SamiProperties.class)
public class SamiConfig {

    @Bean
    DiccionarioSami diccionarioSami(ObjectMapper objectMapper) {
        String json = RecursoTexto.contenido(new ClassPathResource("ia/sami-diccionario.json"));
        return objectMapper.readValue(json, DiccionarioSami.class);
    }
}
