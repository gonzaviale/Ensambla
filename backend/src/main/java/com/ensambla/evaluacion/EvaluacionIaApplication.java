package com.ensambla.evaluacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

public class EvaluacionIaApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication application = new SpringApplication(ConfiguracionEvaluacion.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run(args)) {
            Environment environment = context.getEnvironment();
            context.getBean(EvaluacionIaApplication.class).ejecutar(context, environment);
        }
    }

    public void ejecutar(ConfigurableApplicationContext context, Environment environment) throws IOException {
        Path entrada = Path.of(environment.getRequiredProperty("evaluacion.entrada"));
        Path salida = Path.of(environment.getRequiredProperty("evaluacion.salida"));
        Duration timeout = environment.getProperty("ensambla.ia.timeout", Duration.class, Duration.ofSeconds(60));
        context.getBean(LecturaEvaluacion.class).ejecutar(entrada, salida, timeout);
    }
}
