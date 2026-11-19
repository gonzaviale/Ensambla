package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanRepositoryTest {

    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);

    private Plan esperado;
    private Plan planObtenido;
    private Throwable excepcion;

    @Test
    @DisplayName("Obtiene el plan del catálogo por su código")
    void obtieneElPlanDelCatalogoPorSuCodigo() {
        dadoUnPlanGratuitoEnElCatalogo();

        cuandoSeObtieneElPlanGratuito();

        entoncesSeDevuelveElPlanDelCatalogo();
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el plan no está en el catálogo")
    void respondeRecursoInexistenteCuandoElPlanNoEstaEnElCatalogo() {
        dadoUnCatalogoSinElPlanPremium();

        cuandoSeIntentaObtenerElPlanPremium();

        entoncesSeRechazaPorqueElPlanNoSeEncontro();
    }

    // --- dado ---
    private void dadoUnPlanGratuitoEnElCatalogo() {
        esperado = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(esperado));
    }

    private void dadoUnCatalogoSinElPlanPremium() {
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeObtieneElPlanGratuito() {
        planObtenido = planRepository.obtener(CodigoPlan.GRATUITO);
    }

    private void cuandoSeIntentaObtenerElPlanPremium() {
        excepcion = catchThrowable(() -> planRepository.obtener(CodigoPlan.PREMIUM));
    }

    // --- entonces ---
    private void entoncesSeDevuelveElPlanDelCatalogo() {
        assertThat(planObtenido).isSameAs(esperado);
    }

    private void entoncesSeRechazaPorqueElPlanNoSeEncontro() {
        assertThat(excepcion).isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }
}
