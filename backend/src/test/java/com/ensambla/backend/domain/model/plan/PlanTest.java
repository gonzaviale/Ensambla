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
    @DisplayName("Permite el primer sistema y rechaza el siguiente al alcanzar el límite")
    void validarProyectosShouldEnforceLimit() {
        Plan plan = plan();
        plan.setLimiteProyectos(1);

        assertThatCode(() -> plan.validarProyectos(0)).doesNotThrowAnyException();

        assertThatThrownBy(() -> plan.validarProyectos(1))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("1 sistemas");
    }

    @Test
    @DisplayName("Un límite nulo permite sistemas sin límite comercial")
    void validarProyectosShouldAllowUnlimitedConfiguredPlan() {
        Plan plan = plan();
        plan.setLimiteProyectos(null);

        assertThatCode(() -> plan.validarProyectos(10000)).doesNotThrowAnyException();
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
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("10 páginas de IA por mes");
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
