package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Cableado de Sami: el diccionario de {@code ia/sami-diccionario.json}, el catálogo de componentes y los kits por rubro
 * del taller (si alguno está mal formado no levanta) y el respaldo con IA. El proveedor es el global
 * ({@code ensambla.ia.proveedor}) salvo que Sami lo pise ({@code ENSAMBLA_SAMI_IA_PROVEEDOR}); ver
 * {@link SamiProperties.Ia#ajustes(IaProperties)}.
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

    /** Del modelo rápido al avanzado (si hay): el adapter escala sólo si el rápido no está seguro. */
    @Bean
    SamiIaPort samiIaPort(PromptSami promptSami, ObjectMapper objectMapper, SamiProperties properties,
                          IaProperties iaProperties, ProveedoresIa proveedores) {
        SamiProperties.Ia ia = properties.ia();
        AjustesIa ajustes = ia.ajustes(iaProperties);
        ProveedorIa proveedor = proveedores.elegir(ajustes.proveedor());
        List<ClienteModeloTexto> modelos = new ArrayList<>();
        modelos.add(proveedor.texto(ajustes, ajustes.modeloRapido(), ia.maxTokens()));
        if (!AjustesIa.vacio(ajustes.modeloAvanzado())) {
            modelos.add(proveedor.texto(ajustes, ajustes.modeloAvanzado(), ia.maxTokens()));
        }
        return new SamiIaAdapter(modelos, promptSami, objectMapper, ia.umbralEscalar());
    }

    /**
     * Los componentes del editor que Sami puede ensamblar. El archivo se genera desde el frontend
     * ({@code npm run catalogo:sami}); si está mal formado la aplicación no levanta.
     */
    @Bean
    CatalogoComponentes catalogoComponentes(ObjectMapper objectMapper) {
        String json = RecursoTexto.contenido(new ClassPathResource("ia/catalogo-componentes.json"));
        return objectMapper.readValue(json, CatalogoComponentes.class);
    }

    /** Los sistemas listos por rubro de {@code ia/kits-rubro.json}; si está mal formado la aplicación no levanta. */
    @Bean
    KitsRubro kitsRubro(ObjectMapper objectMapper) {
        String json = RecursoTexto.contenido(new ClassPathResource("ia/kits-rubro.json"));
        return new KitsRubro(List.of(objectMapper.readValue(json, KitRubro[].class)));
    }

    @Bean
    PromptTaller promptTaller(CatalogoComponentes catalogoComponentes, KitsRubro kitsRubro) {
        return new PromptTaller(new ClassPathResource("ia/prompt-taller.md"), catalogoComponentes, kitsRubro);
    }

    /**
     * IA del taller conversacional: una sola llamada por turno al modelo rápido, con las herramientas de
     * {@code ia/herramientas-taller.json}. Con el proveedor simulado no cuesta nada.
     */
    @Bean
    TallerIaPort tallerIaPort(PromptTaller promptTaller, ObjectMapper objectMapper, SamiProperties properties,
                              IaProperties iaProperties, ProveedoresIa proveedores) {
        SamiProperties.Ia ia = properties.ia();
        AjustesIa ajustes = ia.ajustes(iaProperties);
        List<DefinicionHerramienta> herramientas = List.of(objectMapper.readValue(
                RecursoTexto.contenido(new ClassPathResource("ia/herramientas-taller.json")),
                DefinicionHerramienta[].class));
        ClienteModeloConHerramientas modelo = proveedores.elegir(ajustes.proveedor())
                .conHerramientas(ajustes, ia.maxTokensTaller(), herramientas);
        return new TallerIaAdapter(modelo, promptTaller);
    }
}
