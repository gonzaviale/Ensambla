package com.ensambla.backend.domain.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoPlanesMapperTest {

    private final CatalogoPlanesMapper catalogoPlanesMapper = new CatalogoPlanesMapper();

    @Test
    @DisplayName("Mapea todos los campos públicos del catálogo")
    void toResponseShouldMapEveryPublicField() {
        var planes = List.of(plan());

        var result = catalogoPlanesMapper.toResponse(planes);

        thenPlanCompleto(result.getFirst());
    }

    @Test
    @DisplayName("Conserva los indicadores sin configurar y los límites nulos")
    void toResponseShouldPreserveUnconfiguredCatalog() {
        var plan = plan();
        plan.setConfigurado(null);
        plan.setLimitesConfigurados(false);
        plan.setLimiteProyectos(null);

        var result = catalogoPlanesMapper.toResponse(List.of(plan)).getFirst();

        assertThat(result.precioConfigurado()).isFalse();
        assertThat(result.limitesConfigurados()).isFalse();
        assertThat(result.limiteProyectos()).isNull();
    }

    @Test
    @DisplayName("Mapea una lista vacía sin agregar planes")
    void toResponseShouldPreserveEmptyList() {
        var result = catalogoPlanesMapper.toResponse(List.of());

        assertThat(result).isEmpty();
    }

    // --- assert ---
    private void thenPlanCompleto(PlanItem result) {
        assertThat(result.codigo()).isEqualTo("GRATUITO");
        assertThat(result.nombre()).isEqualTo("Gratuito de prueba");
        assertThat(result.precioConfigurado()).isTrue();
        assertThat(result.limitesConfigurados()).isTrue();
        assertThat(result.precioMensual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.moneda()).isEqualTo("ARS");
        assertThat(result.limiteProyectos()).isEqualTo(3);
        assertThat(result.limiteRegistros()).isEqualTo(100L);
        assertThat(result.limitePaginasIaMensuales()).isEqualTo(10);
        assertThat(result.funciones()).containsExactly("FUNCION_DE_PRUEBA");
    }
}
