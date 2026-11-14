package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricasConfigTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final MetricasConfig config = new MetricasConfig();

    @Test
    @DisplayName("Carga el catálogo con métricas para todos y para cada rubro del comercio")
    void catalogoMetricasShouldLoadBaseAndEachRubro() {
        CatalogoMetricas catalogo = config.catalogoMetricas(objectMapper);

        assertThat(catalogo.base()).isNotEmpty();
        assertThat(catalogo.porRubro()).containsOnlyKeys(RubroNegocio.ALMACEN, RubroNegocio.KIOSCO,
                RubroNegocio.MINIMERCADO, RubroNegocio.DIETETICA, RubroNegocio.PAPELERIA);
        assertThat(catalogo.paraRubro(RubroNegocio.OTRO)).isEqualTo(catalogo.base());
        assertThat(catalogo.paraRubro(RubroNegocio.KIOSCO).size()).isGreaterThan(catalogo.base().size());
    }

    @Test
    @DisplayName("Crea el motor de métricas")
    void motorMetricasShouldCreateTheEngine() {
        assertThat(config.motorMetricas()).isInstanceOf(MotorMetricas.class);
    }

    @Test
    @DisplayName("No levanta si el catálogo no se puede leer")
    void cargarShouldFailWhenFileIsMissing() {
        assertThatThrownBy(() -> MetricasConfig.cargar(new ClassPathResource("metricas/no-existe.json"), objectMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catálogo de métricas");
    }

    @Test
    @DisplayName("No levanta si una métrica base del catálogo no es válida")
    void cargarShouldFailWhenABaseMetricIsInvalid() {
        String json = """
                {"base": [{"nombre": "Mal", "fuente": "GASTOS", "calculo": "UNIDADES"}]}
                """;

        assertThatThrownBy(() -> MetricasConfig.cargar(recurso(json), objectMapper))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("No levanta si una métrica de un rubro no es válida")
    void cargarShouldFailWhenARubroMetricIsInvalid() {
        String json = """
                {"porRubro": {"KIOSCO": [{"nombre": "Mal", "fuente": "VENTAS", "calculo": "SUMA",
                                          "agrupar": "CATEGORIA"}]}}
                """;

        assertThatThrownBy(() -> MetricasConfig.cargar(recurso(json), objectMapper))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static ByteArrayResource recurso(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }
}
