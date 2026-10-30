package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes.ListarCatalogoPlanesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse.PlanResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoPlanPresentationMapperTest {

    private final PlanPresentationMapper planPresentationMapper = new PlanPresentationMapper();

    @Test
    @DisplayName("Devuelve todos los campos públicos y solo datos comerciales")
    void toResponseShouldMapEveryPublicField() {
        ListarCatalogoPlanesResponse origen = ListarCatalogoPlanesResponse.de(List.of(plan()));

        List<PlanResponse> result = planPresentationMapper.toResponse(origen);

        thenCamposCompletos(result.getFirst());
    }

    @Test
    @DisplayName("Mapea un catálogo vacío sin agregar datos")
    void toResponseShouldPreserveEmptyList() {
        ListarCatalogoPlanesResponse origen = ListarCatalogoPlanesResponse.de(List.of());

        List<PlanResponse> result = planPresentationMapper.toResponse(origen);

        assertThat(result).isEmpty();
    }

    // --- assert ---
    private void thenCamposCompletos(PlanResponse result) {
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
