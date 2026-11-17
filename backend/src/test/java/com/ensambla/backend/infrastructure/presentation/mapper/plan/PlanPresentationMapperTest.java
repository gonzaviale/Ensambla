package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PlanPresentationMapperTest {

    private final PlanPresentationMapper planPresentationMapper = new PlanPresentationMapper();
    private ConsultarPlanesResponse origen;
    private PlanesResponse planes;
    private IniciarPruebaRequest pedido;
    private PruebaResponse prueba;

    @Test
    @DisplayName("El contrato HTTP de planes conserva todos los campos e historial")
    void toResponseShouldMapPlansWithHistory() {
        givenUnaConsultaConHistorial();

        whenSeMapeanLosPlanes();

        thenSeConservanLosPlanes();
        thenSeConservanSuscripcionYUso();
        thenSeConservaElHistorialDePrueba();
    }

    @Test
    @DisplayName("El contrato HTTP no inventa una prueba si no existe historial")
    void toResponseShouldKeepMissingTrialHistory() {
        givenUnaConsultaSinHistorial();

        whenSeMapeanLosPlanes();

        thenNoHayPrueba();
    }

    @Test
    @DisplayName("El pedido recibe exclusivamente el identificador del usuario autenticado")
    void toRequestShouldMapAuthenticatedUser() {
        whenSeArmaElPedido();

        thenElPedidoTraeElUsuario();
    }

    @Test
    @DisplayName("La respuesta HTTP contiene fechas y cuotas de la prueba")
    void toResponseShouldMapEveryField() {
        whenSeMapeaLaPrueba();

        thenLaPruebaTraeFechasYCuotas();
    }

    // --- arrange ---
    private void givenUnaConsultaConHistorial() {
        origen = ConsultarPlanesResponse.de(consulta(true), AHORA);
    }

    private void givenUnaConsultaSinHistorial() {
        origen = ConsultarPlanesResponse.de(consulta(false), AHORA);
    }

    // --- act ---
    private void whenSeMapeanLosPlanes() {
        planes = planPresentationMapper.toResponse(origen);
    }

    private void whenSeArmaElPedido() {
        pedido = planPresentationMapper.toRequest(USUARIO_ID);
    }

    private void whenSeMapeaLaPrueba() {
        prueba = planPresentationMapper.toResponse(new PruebaPremiumResponse(INSTANTE, INSTANTE.plusDays(7), 2, 10));
    }

    // --- assert ---
    private void thenSeConservanLosPlanes() {
        assertThat(planes.consultadoEn()).isEqualTo(origen.consultadoEn());
        assertThat(planes.planes()).hasSize(2);
        IntStream.range(0, origen.planes().size()).forEach(this::thenElPlanSeConserva);
    }

    private void thenElPlanSeConserva(int indice) {
        var esperado = origen.planes().get(indice);
        var actual = planes.planes().get(indice);
        assertThat(actual.codigo()).isEqualTo(esperado.codigo());
        assertThat(actual.nombre()).isEqualTo(esperado.nombre());
        assertThat(actual.precioConfigurado()).isEqualTo(esperado.precioConfigurado());
        assertThat(actual.limitesConfigurados()).isEqualTo(esperado.limitesConfigurados());
        assertThat(actual.precioMensual()).isEqualTo(esperado.precioMensual());
        assertThat(actual.moneda()).isEqualTo(esperado.moneda());
        assertThat(actual.limiteProyectos()).isEqualTo(esperado.limiteProyectos());
        assertThat(actual.limiteRegistros()).isEqualTo(esperado.limiteRegistros());
        assertThat(actual.limitePaginasIaMensuales()).isEqualTo(esperado.limitePaginasIaMensuales());
        assertThat(actual.limiteObjetivosIaMensuales()).isEqualTo(esperado.limiteObjetivosIaMensuales());
        assertThat(actual.funciones()).isEqualTo(esperado.funciones());
    }

    private void thenSeConservanSuscripcionYUso() {
        assertThat(planes.suscripcion().codigoPlanActual()).isEqualTo(origen.suscripcion().codigoPlanActual());
        assertThat(planes.suscripcion().pruebaActiva()).isEqualTo(origen.suscripcion().pruebaActiva());
        assertThat(planes.suscripcion().puedeIniciarPrueba()).isEqualTo(origen.suscripcion().puedeIniciarPrueba());
        assertThat(planes.suscripcion().inicioEn()).isEqualTo(origen.suscripcion().inicioEn());
        assertThat(planes.suscripcion().finEn()).isEqualTo(origen.suscripcion().finEn());
        assertThat(planes.uso().proyectos()).isEqualTo(origen.uso().proyectos());
        assertThat(planes.uso().registros()).isEqualTo(origen.uso().registros());
        assertThat(planes.uso().paginasIa()).isEqualTo(origen.uso().paginasIa());
        assertThat(planes.uso().limitePaginasIa()).isEqualTo(origen.uso().limitePaginasIa());
        assertThat(planes.uso().periodoIa()).isEqualTo(origen.uso().periodoIa());
        assertThat(planes.uso().objetivosIa()).isEqualTo(origen.uso().objetivosIa());
        assertThat(planes.uso().limiteObjetivosIa()).isEqualTo(origen.uso().limiteObjetivosIa());
        assertThat(planes.uso().inicioPeriodoIa()).isEqualTo(origen.uso().inicioPeriodoIa());
        assertThat(planes.uso().finPeriodoIa()).isEqualTo(origen.uso().finPeriodoIa());
    }

    private void thenSeConservaElHistorialDePrueba() {
        assertThat(planes.prueba().inicioEn()).isEqualTo(origen.prueba().inicioEn());
        assertThat(planes.prueba().finEn()).isEqualTo(origen.prueba().finEn());
        assertThat(planes.prueba().paginasIaUtilizadas()).isEqualTo(origen.prueba().paginasIaUtilizadas());
        assertThat(planes.prueba().limitePaginasIa()).isEqualTo(origen.prueba().limitePaginasIa());
    }

    private void thenNoHayPrueba() {
        assertThat(planes.prueba()).isNull();
    }

    private void thenElPedidoTraeElUsuario() {
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void thenLaPruebaTraeFechasYCuotas() {
        assertThat(prueba.inicioEn()).isEqualTo(INSTANTE);
        assertThat(prueba.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(prueba.paginasIaUtilizadas()).isEqualTo(2);
        assertThat(prueba.limitePaginasIa()).isEqualTo(10);
    }
}
