package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanTest {

    @Test
    @DisplayName("El gratuito permite el primer negocio y rechaza el segundo sugiriendo Premium")
    void validarNegociosShouldEnforceSingularLimitAndSuggestPremium() {
        Plan plan = plan();
        plan.setLimiteNegocios(1);

        assertThatCode(() -> plan.validarNegocios(0)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarNegocios(1))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Tu plan permite hasta 1 negocio. Pasate a Premium para sumar más.")
                .extracting("codigo").isEqualTo("LIMITE_NEGOCIOS");
    }

    @Test
    @DisplayName("Premium permite hasta seis negocios y no se sugiere a sí mismo")
    void validarNegociosShouldEnforcePluralLimitWithoutSuggestion() {
        Plan plan = plan();
        plan.setCodigo(CodigoPlan.PREMIUM);
        plan.setLimiteNegocios(6);

        assertThatCode(() -> plan.validarNegocios(5)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarNegocios(6))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Tu plan permite hasta 6 negocios.")
                .extracting("codigo").isEqualTo("LIMITE_NEGOCIOS");
    }

    @Test
    @DisplayName("Un límite nulo permite negocios sin límite comercial")
    void validarNegociosShouldAllowUnlimitedConfiguredPlan() {
        Plan plan = plan();
        plan.setLimiteNegocios(null);

        assertThatCode(() -> plan.validarNegocios(10000)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un límite de registros nulo permite registros sin límite comercial")
    void validarRegistrosShouldAllowUnlimitedConfiguredPlan() {
        Plan plan = plan();
        plan.setLimiteRegistros(null);

        assertThatCode(() -> plan.validarRegistros(1_000_000, 1000)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valida todas las filas del lote antes de permitir el alta")
    void validarRegistrosShouldCheckWholeBatch() {
        Plan plan = plan();
        plan.setLimiteRegistros(1000L);

        assertThatCode(() -> plan.validarRegistros(998, 2)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarRegistros(998, 3))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("1000 registros");
    }

    @Test
    @DisplayName("Un lote vacío no exige eliminar registros cuando el negocio ya superó su cuota")
    void validarRegistrosShouldAllowEmptyBatchAboveLimit() {
        Plan plan = plan();
        plan.setLimiteRegistros(1000L);

        assertThatCode(() -> plan.validarRegistros(1500, 0)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarRegistros(1500, 1))
                .isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("La cuota mensual de IA incluye páginas procesadas y reservadas")
    void validarPaginasIaShouldEnforceMonthlyAllowance() {
        Plan plan = plan();
        plan.setLimitePaginasIaMensuales(10);

        assertThatCode(() -> plan.validarPaginasIa(9, 1)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarPaginasIa(9, 2))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("10 análisis de foto por mes");
    }

    @Test
    @DisplayName("No habilita límites comerciales pendientes de configuración")
    void validarLimitesConfiguradosShouldFailClosedWhenFalse() {
        Plan plan = plan();
        plan.setLimitesConfigurados(false);

        assertThatThrownBy(plan::validarLimitesConfigurados).isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Los límites del plan no están configurados");
    }

    @Test
    @DisplayName("No interpreta una configuración nula como un plan ilimitado")
    void validarLimitesConfiguradosShouldFailClosedWhenNull() {
        Plan plan = plan();
        plan.setLimitesConfigurados(null);

        assertThatThrownBy(plan::validarLimitesConfigurados).isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("Permite usar capacidades aprobadas aunque el precio siga pendiente")
    void validarLimitesConfiguradosShouldAllowCapabilitiesWithoutPrice() {
        Plan plan = plan();
        plan.setConfigurado(false);

        assertThatCode(plan::validarLimitesConfigurados).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Permite sólo las funciones incluidas explícitamente en el catálogo")
    void validarFuncionShouldRespectEnabledFeatures() {
        Plan plan = plan();
        plan.setFunciones(List.of(FuncionPlan.RECETAS_COSTOS.name()));

        assertThatCode(() -> plan.validarFuncion(FuncionPlan.RECETAS_COSTOS)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarFuncion(FuncionPlan.EXPORTAR_EXCEL))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("premium activo");
    }
}
