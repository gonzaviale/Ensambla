package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.service.plan.CatalogoPlanesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoPlanesUseCaseConfigTest {

    @Test
    @DisplayName("Conecta el servicio y el mapper real del catálogo")
    void listarCatalogoPlanesUseCaseShouldWireDependencies() {
        var config = new PlanUseCaseConfig();
        var catalogoPlanesService = mock(CatalogoPlanesService.class);
        when(catalogoPlanesService.listar()).thenReturn(List.of(plan()));

        var result = config.listarCatalogoPlanesUseCase(catalogoPlanesService,
                config.catalogoPlanesMapper()).execute();

        assertThat(result.getFirst().nombre()).isEqualTo("Gratuito de prueba");
    }
}
