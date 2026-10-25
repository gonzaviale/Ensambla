package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultarPlanesResponseTest {

    @Test
    @DisplayName("Mapea catálogo completo, suscripción efectiva, uso e historial de prueba")
    void deShouldMapEveryField() {
        ConsultaPlanes consulta = consulta(true);

        ConsultarPlanesResponse result = ConsultarPlanesResponse.de(consulta, AHORA);

        assertThat(result.consultadoEn()).isEqualTo(AHORA);
        assertThat(result.planes()).hasSize(2);
        for (int indice = 0; indice < consulta.planes().size(); indice++) {
            thenPlanMatches(result.planes().get(indice), consulta.planes().get(indice));
        }
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.suscripcion().inicioEn()).isEqualTo(consulta.suscripcion().getInicioEn());
        assertThat(result.suscripcion().finEn()).isEqualTo(consulta.suscripcion().getFinEn());
        assertThat(result.uso().proyectos()).isEqualTo(2);
        assertThat(result.uso().registros()).isEqualTo(89);
        assertThat(result.uso().paginasIa()).isEqualTo(6);
        assertThat(result.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(result.uso().periodoIa()).isEqualTo("MENSUAL");
        assertThat(result.uso().inicioPeriodoIa()).isEqualTo(consulta.inicioPeriodoIa());
        assertThat(result.uso().finPeriodoIa()).isEqualTo(consulta.finPeriodoIa());
        assertThat(result.prueba().inicioEn()).isEqualTo(consulta.prueba().inicioEn());
        assertThat(result.prueba().finEn()).isEqualTo(consulta.prueba().finEn());
        assertThat(result.prueba().paginasIaUtilizadas()).isEqualTo(8);
        assertThat(result.prueba().limitePaginasIa()).isEqualTo(10);
    }

    @Test
    @DisplayName("Un negocio que nunca inició prueba conserva historial nulo y elegibilidad")
    void deShouldPreserveAbsentHistory() {
        ConsultarPlanesResponse result = ConsultarPlanesResponse.de(consulta(false), AHORA);

        assertThat(result.prueba()).isNull();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    // --- assert ---
    private void thenPlanMatches(PlanItem actual, Plan esperado) {
        assertThat(actual.codigo()).isEqualTo(esperado.getCodigo().name());
        assertThat(actual.nombre()).isEqualTo(esperado.getNombre());
        assertThat(actual.precioConfigurado()).isEqualTo(esperado.getConfigurado());
        assertThat(actual.limitesConfigurados()).isEqualTo(esperado.getLimitesConfigurados());
        assertThat(actual.precioMensual()).isEqualTo(esperado.getPrecioMensual());
        assertThat(actual.moneda()).isEqualTo(esperado.getMoneda());
        assertThat(actual.limiteProyectos()).isEqualTo(esperado.getLimiteProyectos());
        assertThat(actual.limiteRegistros()).isEqualTo(esperado.getLimiteRegistros());
        assertThat(actual.limitePaginasIaMensuales()).isEqualTo(esperado.getLimitePaginasIaMensuales());
        assertThat(actual.funciones()).isEqualTo(esperado.getFunciones());
    }
}
