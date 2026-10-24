package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SuscripcionServiceTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final SuscripcionService suscripcionService = new SuscripcionService(planRepository, suscripcionRepository);

    @Test
    @DisplayName("Obtiene la suscripción solamente por el negocio recibido del servidor")
    void obtenerShouldReturnSubscriptionOfNegocio() {
        Suscripcion esperada = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(esperada));

        Suscripcion result = suscripcionService.obtener(NEGOCIO_ID);

        assertThat(result).isSameAs(esperada);
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void obtenerShouldRejectMissingSubscription() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.obtener(NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Obtiene el plan del catálogo por su código")
    void obtenerPlanShouldReturnPlanOfCatalog() {
        Plan esperado = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(esperado));

        assertThat(suscripcionService.obtenerPlan(CodigoPlan.GRATUITO)).isSameAs(esperado);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el plan no está en el catálogo")
    void obtenerPlanShouldRejectMissingPlan() {
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.obtenerPlan(CodigoPlan.PREMIUM))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan no encontrado");
    }
}
