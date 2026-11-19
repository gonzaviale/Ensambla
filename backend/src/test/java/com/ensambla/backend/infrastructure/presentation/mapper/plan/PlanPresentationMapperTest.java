package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PlanPresentationMapperTest {

    private final PlanPresentationMapper mapper = new PlanPresentationMapper();
    private ConsultarPlanesResponse consulta;
    private PlanesResponse planes;

    @Test
    @DisplayName("El contrato HTTP de planes conserva todos los campos de cada plan")
    void elContratoDePlanesConservaLosCamposDeCadaPlan() {
        dadaLaConsultaDePlanes(true);

        cuandoSeRespondenLosPlanes();

        entoncesSeConservanLosPlanes();
    }

    @Test
    @DisplayName("El contrato HTTP de planes conserva la suscripción, el uso y el historial de la prueba")
    void elContratoDePlanesConservaLaSuscripcionElUsoYLaPrueba() {
        dadaLaConsultaDePlanes(true);

        cuandoSeRespondenLosPlanes();

        entoncesSeConservanLaSuscripcionElUsoYLaPrueba();
    }

    @Test
    @DisplayName("El contrato HTTP no inventa una prueba si no existe historial")
    void elContratoNoInventaUnaPruebaSinHistorial() {
        dadaLaConsultaDePlanes(false);

        cuandoSeRespondenLosPlanes();

        entoncesNoHayPrueba();
    }

    @Test
    @DisplayName("El pedido recibe exclusivamente el identificador del usuario autenticado")
    void elPedidoRecibeSoloElUsuarioAutenticado() {
        entoncesElPedidoEsDe(new IniciarPruebaRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("La respuesta HTTP contiene fechas y cuotas de la prueba")
    void laRespuestaContieneFechasYCuotasDeLaPrueba() {
        entoncesLaPruebaSeRespondeComo(new PruebaPremiumResponse(INSTANTE, INSTANTE.plusDays(7), 2, 10),
                new PruebaResponse(INSTANTE, INSTANTE.plusDays(7), 2, 10));
    }

    private void dadaLaConsultaDePlanes(boolean conPrueba) {
        consulta = ConsultarPlanesResponse.de(consulta(conPrueba), AHORA);
    }

    private void cuandoSeRespondenLosPlanes() {
        planes = mapper.toResponse(consulta);
    }

    private void entoncesSeConservanLosPlanes() {
        assertThat(planes.consultadoEn()).isEqualTo(consulta.consultadoEn());
        assertThat(planes.planes()).usingRecursiveComparison().isEqualTo(consulta.planes());
    }

    private void entoncesSeConservanLaSuscripcionElUsoYLaPrueba() {
        assertThat(planes.suscripcion()).usingRecursiveComparison().isEqualTo(consulta.suscripcion());
        assertThat(planes.uso()).usingRecursiveComparison().isEqualTo(consulta.uso());
        assertThat(planes.prueba()).usingRecursiveComparison().isEqualTo(consulta.prueba());
    }

    private void entoncesNoHayPrueba() {
        assertThat(planes.prueba()).isNull();
    }

    private void entoncesElPedidoEsDe(IniciarPruebaRequest esperado) {
        assertThat(mapper.toRequest(USUARIO_ID)).isEqualTo(esperado);
    }

    private void entoncesLaPruebaSeRespondeComo(PruebaPremiumResponse prueba, PruebaResponse esperada) {
        assertThat(mapper.toResponse(prueba)).isEqualTo(esperada);
    }
}