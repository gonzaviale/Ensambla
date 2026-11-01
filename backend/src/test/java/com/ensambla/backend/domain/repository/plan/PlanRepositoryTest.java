package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanRepositoryTest {

    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene el plan del catálogo por su código")
    void obtenerShouldReturnPlanOfCatalog() {
        Plan esperado = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(esperado));

        assertThat(planRepository.obtener(CodigoPlan.GRATUITO)).isSameAs(esperado);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el plan no está en el catálogo")
    void obtenerShouldRejectMissingPlan() {
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planRepository.obtener(CodigoPlan.PREMIUM))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan no encontrado");
    }
}
