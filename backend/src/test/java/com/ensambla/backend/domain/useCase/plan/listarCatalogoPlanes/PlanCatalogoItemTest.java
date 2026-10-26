package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.ensambla.backend.support.PlanTestData.plan;
import static org.assertj.core.api.Assertions.assertThat;

class PlanCatalogoItemTest {

    @Test
    @DisplayName("Copia todos los campos comerciales sin ids ni datos de negocios")
    void deShouldCopyEveryCommercialField() {
        Plan origen = plan();

        PlanCatalogoItem result = PlanCatalogoItem.de(origen);

        thenCamposCompletos(result);
    }

    @Test
    @DisplayName("Conserva precios y límites nulos sin asumir condiciones comerciales")
    void deShouldPreserveUnconfiguredValues() {
        Plan origen = givenPlanSinConfigurar();

        PlanCatalogoItem result = PlanCatalogoItem.de(origen);

        thenSinConfigurar(result);
    }

    // --- arrange ---
    private Plan givenPlanSinConfigurar() {
        Plan origen = plan();
        origen.setConfigurado(null);
        origen.setLimitesConfigurados(false);
        origen.setPrecioMensual(null);
        origen.setMoneda(null);
        origen.setLimiteProyectos(null);
        origen.setLimiteRegistros(null);
        origen.setLimitePaginasIaMensuales(null);
        return origen;
    }

    // --- assert ---
    private void thenCamposCompletos(PlanCatalogoItem result) {
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

    private void thenSinConfigurar(PlanCatalogoItem result) {
        assertThat(result.precioConfigurado()).isFalse();
        assertThat(result.limitesConfigurados()).isFalse();
        assertThat(result.precioMensual()).isNull();
        assertThat(result.moneda()).isNull();
        assertThat(result.limiteProyectos()).isNull();
        assertThat(result.limiteRegistros()).isNull();
        assertThat(result.limitePaginasIaMensuales()).isNull();
    }
}
