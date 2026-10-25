package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PlanItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoPlanesPresentationMapperTest {

    private final CatalogoPlanesPresentationMapper catalogoPlanesPresentationMapper =
            new CatalogoPlanesPresentationMapper();

    @Test
    @DisplayName("Mapea todos los campos comerciales sin datos privados")
    void toResponseShouldMapEveryField() {
        var plan = new PlanItem("PREMIUM", "Premium", true, true, BigDecimal.TEN, "ARS",
                null, null, 100, List.of("REPORTES_FILTRADOS"));

        var result = catalogoPlanesPresentationMapper.toResponse(List.of(plan)).getFirst();

        assertThat(result.codigo()).isEqualTo("PREMIUM");
        assertThat(result.nombre()).isEqualTo("Premium");
        assertThat(result.precioConfigurado()).isTrue();
        assertThat(result.limitesConfigurados()).isTrue();
        assertThat(result.precioMensual()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.moneda()).isEqualTo("ARS");
        assertThat(result.limiteProyectos()).isNull();
        assertThat(result.limiteRegistros()).isNull();
        assertThat(result.limitePaginasIaMensuales()).isEqualTo(100);
        assertThat(result.funciones()).containsExactly("REPORTES_FILTRADOS");
    }

    @Test
    @DisplayName("Conserva una lista vacía sin planes inventados")
    void toResponseShouldPreserveEmptyList() {
        var result = catalogoPlanesPresentationMapper.toResponse(List.of());

        assertThat(result).isEmpty();
    }
}
