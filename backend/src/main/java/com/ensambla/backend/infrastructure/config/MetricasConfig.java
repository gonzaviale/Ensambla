package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class MetricasConfig {

    /** Las sugeridas de {@code metricas/catalogo-metricas.json}; si alguna no es válida, la aplicación no levanta. */
    @Bean
    public CatalogoMetricas catalogoMetricas(ObjectMapper objectMapper) {
        return cargar(new ClassPathResource("metricas/catalogo-metricas.json"), objectMapper);
    }

    @Bean
    public MotorMetricas motorMetricas() {
        return new MotorMetricas();
    }

    static CatalogoMetricas cargar(Resource recurso, ObjectMapper objectMapper) {
        CatalogoMetricas catalogo = leer(recurso, objectMapper).aCatalogo();
        validarTodas(catalogo);
        return catalogo;
    }

    private static CatalogoMetricasJson leer(Resource recurso, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(recurso.getContentAsString(StandardCharsets.UTF_8),
                    CatalogoMetricasJson.class);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el catálogo de métricas " + recurso.getDescription(), e);
        }
    }

    private static void validarTodas(CatalogoMetricas catalogo) {
        catalogo.base().forEach(DefinicionMetrica::validar);
        catalogo.porRubro().values().forEach(definiciones -> definiciones.forEach(DefinicionMetrica::validar));
    }
}
