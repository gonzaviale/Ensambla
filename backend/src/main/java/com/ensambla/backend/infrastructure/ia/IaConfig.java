package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Cableado de la IA. Los proveedores ({@link ProveedorIa}) arman los clientes; {@code ensambla.ia.proveedor} elige cuál
 * atiende la lectura de cuadernos (y a Sami, si no lo pisa). Para sumar un proveedor alcanza con implementar
 * {@link ProveedorIa} y registrarlo acá como bean; sus credenciales y modelos van en
 * {@code ensambla.ia.proveedores.<nombre>.*}. El resto (prompt, esquema, validación y consumo) ya lo resuelve
 * {@link LectorCuadernoIaAdapter}. Un proveedor que no se elige no se conecta a nada.
 */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    /** Los kits los carga {@link SamiConfig}; se piden recién al armar el taller (la evaluación de lectura no los usa). */
    @Bean
    ProveedorIaSimulado proveedorIaSimulado(ObjectProvider<KitsRubro> kitsRubro) {
        return new ProveedorIaSimulado(new ClassPathResource("ia/lectura-simulada.json"), kitsRubro::getObject);
    }

    @Bean
    ProveedorIaAnthropic proveedorIaAnthropic() {
        return new ProveedorIaAnthropic();
    }

    @Bean
    ProveedorIaBedrock proveedorIaBedrock() {
        return new ProveedorIaBedrock();
    }

    @Bean
    ProveedoresIa proveedoresIa(List<ProveedorIa> proveedores) {
        return new ProveedoresIa(proveedores);
    }

    @Bean
    ClienteModeloVision clienteModeloVision(ProveedoresIa proveedores, IaProperties properties) {
        AjustesIa ajustes = properties.ajustes();
        return proveedores.elegir(ajustes.proveedor()).vision(ajustes);
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
