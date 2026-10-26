package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarCatalogoPlanesConfigTest {

    @Test
    @DisplayName("Conecta repositorio y puerto de transacción en el catálogo")
    void listarCatalogoPlanesShouldWireDependencies() {
        PlanRepository planRepository = givenCatalogo();

        var result = new PlanUseCaseConfig().listarCatalogoPlanes(planRepository, new TransaccionDirecta()).execute();

        assertThat(result.planes()).hasSize(2);
    }

    // --- arrange ---
    private PlanRepository givenCatalogo() {
        PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(plan()));
        return planRepository;
    }
}
