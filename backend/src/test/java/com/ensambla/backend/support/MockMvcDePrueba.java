package com.ensambla.backend.support;

import com.ensambla.backend.infrastructure.config.IdentificadoresConfig;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * MockMvc de un controller sin levantar Spring, pero con los ids tipados como en la aplicación: se leen de las rutas
 * y parámetros y viajan en el JSON como el texto de su UUID.
 */
public final class MockMvcDePrueba {

    private MockMvcDePrueba() {
    }

    public static StandaloneMockMvcBuilder standalone(Object... controllers) {
        IdentificadoresConfig identificadores = new IdentificadoresConfig();
        DefaultFormattingConversionService conversiones = new DefaultFormattingConversionService();
        identificadores.addFormatters(conversiones);
        JsonMapper json = JsonMapper.builder().addModule(identificadores.identificadoresModule()).build();
        return MockMvcBuilders.standaloneSetup(controllers)
                .setConversionService(conversiones)
                .setMessageConverters(new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8), new ResourceHttpMessageConverter(),
                        new JacksonJsonHttpMessageConverter(json));
    }
}
