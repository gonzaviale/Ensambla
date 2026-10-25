package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.common.Identificador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Los ids tipados viajan como el texto de su UUID: en el JSON (ida y vuelta) y en las rutas y parámetros de los
 * controllers. El front no nota la diferencia con un UUID.
 */
@Configuration
public class IdentificadoresConfig implements WebMvcConfigurer {

    @Bean
    public JacksonModule identificadoresModule() {
        SimpleModule modulo = new SimpleModule("identificadores");
        modulo.addSerializer(Identificador.class, ToStringSerializer.instance);
        modulo.setDeserializers(new SimpleDeserializers() {
            @Override
            public ValueDeserializer<?> findBeanDeserializer(JavaType tipo, DeserializationConfig config,
                                                             BeanDescription.Supplier descripcion) {
                return Identificador.class.isAssignableFrom(tipo.getRawClass())
                        ? new IdentificadorDeserializer(tipo.getRawClass())
                        : super.findBeanDeserializer(tipo, config, descripcion);
            }
        });
        return modulo;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new ConverterFactory<String, Identificador>() {
            @Override
            public <T extends Identificador> Converter<String, T> getConverter(Class<T> tipo) {
                return texto -> tipo.cast(crear(tipo, texto.strip()));
            }
        });
    }

    /** Arma el id con el {@code de(String)} de su tipo; un texto que no es un UUID se rechaza como argumento inválido. */
    static Identificador crear(Class<?> tipo, String texto) {
        try {
            Method de = tipo.getMethod("de", String.class);
            return (Identificador) de.invoke(null, texto);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException invalido) {
                throw invalido;
            }
            throw new IllegalStateException("No se pudo leer el id " + tipo.getSimpleName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(tipo.getSimpleName() + " no tiene de(String)", e);
        }
    }

    private static final class IdentificadorDeserializer extends FromStringDeserializer<Identificador> {

        private final Class<?> tipo;

        private IdentificadorDeserializer(Class<?> tipo) {
            super(tipo);
            this.tipo = tipo;
        }

        @Override
        protected Identificador _deserialize(String texto, DeserializationContext contexto) {
            return crear(tipo, texto);
        }
    }
}
