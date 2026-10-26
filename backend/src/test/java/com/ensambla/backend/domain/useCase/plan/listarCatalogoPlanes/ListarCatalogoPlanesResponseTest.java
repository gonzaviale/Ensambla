package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;

class ListarCatalogoPlanesResponseTest {

    @Test
    @DisplayName("Mapea el catálogo conservando el orden del repositorio")
    void deShouldPreserveCatalogOrder() {
        Plan premium = plan();
        premium.setCodigo(CodigoPlan.PREMIUM);

        ListarCatalogoPlanesResponse result = ListarCatalogoPlanesResponse.de(List.of(premium, plan()));

        assertThat(result.planes()).extracting(PlanCatalogoItem::codigo).containsExactly("PREMIUM", "GRATUITO");
    }

    @Test
    @DisplayName("No agrega planes cuando la lista está vacía")
    void deShouldPreserveEmptyList() {
        ListarCatalogoPlanesResponse result = ListarCatalogoPlanesResponse.de(List.of());

        assertThat(result.planes()).isEmpty();
    }
}
