package com.ensambla.evaluacion;

import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.infrastructure.ia.IaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Import(IaConfig.class)
public class ConfiguracionEvaluacion {

    @Bean
    public EvaluacionIaApplication evaluacionIaApplication() {
        return new EvaluacionIaApplication();
    }

    @Bean
    public ObjectMapper objectMapperEvaluacion() {
        return JsonMapper.builder().build();
    }

    @Bean
    public LecturaEvaluacion lecturaEvaluacion(LectorCuadernoPort lectorCuadernoPort,
                                               ObjectMapper objectMapperEvaluacion) {
        return new LecturaEvaluacion(lectorCuadernoPort, objectMapperEvaluacion);
    }
}
