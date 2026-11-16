package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanCartelesIaTest {

    private Plan plan;

    @Test
    @DisplayName("Admite un cartel con IA mientras el mes no llegó al límite")
    void validarCartelesIaShouldAllowWhileBelowTheLimit() {
        givenUnPlanConLimite(10);

        assertThatCode(() -> plan.validarCartelesIa(9)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza el cartel con IA cuando se usaron todos los del mes, con su propio código")
    void validarCartelesIaShouldRejectWhenTheMonthIsFull() {
        givenUnPlanConLimite(10);

        assertThatThrownBy(() -> plan.validarCartelesIa(10))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("10 carteles con IA por mes");
    }

    @Test
    @DisplayName("Sin límite configurado no se restringe")
    void validarCartelesIaShouldNotLimitWhenThereIsNoLimit() {
        givenUnPlanConLimite(null);

        assertThatCode(() -> plan.validarCartelesIa(1_000)).doesNotThrowAnyException();
    }

    private void givenUnPlanConLimite(Integer limite) {
        plan = plan();
        plan.setLimiteCartelesIaMensuales(limite);
    }
}
