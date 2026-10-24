package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
class PlanPresentationMapperTest {

    @Test
    @DisplayName("El contrato HTTP de planes conserva todos los campos e historial")
    void toResponseShouldMapPlansWithHistory() {
        var origen = ConsultarPlanesResponse.de(consulta(true), AHORA);

        var result = planPresentationMapper.toResponse(origen);

        assertThat(result.consultadoEn()).isEqualTo(origen.consultadoEn());
        assertThat(result.planes()).hasSize(2);
        for (int indice = 0; indice < origen.planes().size(); indice++) {
            var esperado = origen.planes().get(indice);
            var actual = result.planes().get(indice);
            assertThat(actual.codigo()).isEqualTo(esperado.codigo());
            assertThat(actual.nombre()).isEqualTo(esperado.nombre());
            assertThat(actual.precioConfigurado()).isEqualTo(esperado.precioConfigurado());
            assertThat(actual.limitesConfigurados()).isEqualTo(esperado.limitesConfigurados());
            assertThat(actual.precioMensual()).isEqualTo(esperado.precioMensual());
            assertThat(actual.moneda()).isEqualTo(esperado.moneda());
            assertThat(actual.limiteProyectos()).isEqualTo(esperado.limiteProyectos());
            assertThat(actual.limiteRegistros()).isEqualTo(esperado.limiteRegistros());
            assertThat(actual.limitePaginasIaMensuales()).isEqualTo(esperado.limitePaginasIaMensuales());
            assertThat(actual.funciones()).isEqualTo(esperado.funciones());
        }
        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo(origen.suscripcion().codigoPlanActual());
        assertThat(result.suscripcion().pruebaActiva()).isEqualTo(origen.suscripcion().pruebaActiva());
        assertThat(result.suscripcion().puedeIniciarPrueba()).isEqualTo(origen.suscripcion().puedeIniciarPrueba());
        assertThat(result.suscripcion().inicioEn()).isEqualTo(origen.suscripcion().inicioEn());
        assertThat(result.suscripcion().finEn()).isEqualTo(origen.suscripcion().finEn());
        assertThat(result.uso().proyectos()).isEqualTo(origen.uso().proyectos());
        assertThat(result.uso().registros()).isEqualTo(origen.uso().registros());
        assertThat(result.uso().paginasIa()).isEqualTo(origen.uso().paginasIa());
        assertThat(result.uso().limitePaginasIa()).isEqualTo(origen.uso().limitePaginasIa());
        assertThat(result.uso().periodoIa()).isEqualTo(origen.uso().periodoIa());
        assertThat(result.uso().inicioPeriodoIa()).isEqualTo(origen.uso().inicioPeriodoIa());
        assertThat(result.uso().finPeriodoIa()).isEqualTo(origen.uso().finPeriodoIa());
        assertThat(result.prueba().inicioEn()).isEqualTo(origen.prueba().inicioEn());
        assertThat(result.prueba().finEn()).isEqualTo(origen.prueba().finEn());
        assertThat(result.prueba().paginasIaUtilizadas()).isEqualTo(origen.prueba().paginasIaUtilizadas());
        assertThat(result.prueba().limitePaginasIa()).isEqualTo(origen.prueba().limitePaginasIa());
    }

    @Test
    @DisplayName("El contrato HTTP no inventa una prueba si no existe historial")
    void toResponseShouldKeepMissingTrialHistory() {
        var origen = ConsultarPlanesResponse.de(consulta(false), AHORA);

        var result = planPresentationMapper.toResponse(origen);

        assertThat(result.prueba()).isNull();
    }

    private final PlanPresentationMapper planPresentationMapper = new PlanPresentationMapper();

    @Test
    @DisplayName("El pedido recibe exclusivamente el identificador del usuario autenticado")
    void toRequestShouldMapAuthenticatedUser() {
        IniciarPruebaRequest result = planPresentationMapper.toRequest(NEGOCIO_ID);

        assertThat(result.usuarioId()).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("La respuesta HTTP contiene fechas y cuotas de la prueba")
    void toResponseShouldMapEveryField() {
        PruebaPremiumResponse response = new PruebaPremiumResponse(INSTANTE, INSTANTE.plusDays(7), 2, 10);

        PruebaResponse result = planPresentationMapper.toResponse(response);

        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIaUtilizadas()).isEqualTo(2);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }
}
