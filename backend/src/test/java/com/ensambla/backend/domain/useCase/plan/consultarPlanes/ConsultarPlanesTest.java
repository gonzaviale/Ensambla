package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.ConsultaPlanesTestData.prueba;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarPlanesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("bb000000-0000-4000-8000-000000000009");
    private static final OffsetDateTime PRIMER_MINUTO_DEL_MES_ARGENTINO =
            OffsetDateTime.parse("2026-11-01T00:05:00-03:00");
    private static final OffsetDateTime INICIO_DEL_MES_EN_UTC = OffsetDateTime.parse("2026-11-01T03:00:00Z");
    private static final OffsetDateTime INICIO_DEL_MES_SIGUIENTE_EN_UTC = OffsetDateTime.parse("2026-12-01T03:00:00Z");
    private static final LocalDate INICIO_DEL_MES = LocalDate.parse("2026-11-01");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);
    private ConsultarPlanesResponse respuesta;
    private Throwable error;

    @BeforeEach
    void givenNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El gratuito muestra catálogo, uso del mes argentino y disponibilidad de prueba bajo el mismo bloqueo")
    void executeShouldReturnFreePlanAndArgentinaMonth() {
        givenUnGratuitoConUsoDelMes();

        whenSeConsulta(PRIMER_MINUTO_DEL_MES_ARGENTINO);

        thenSeMuestraElGratuitoConSuCatalogoYSuPrueba();
        thenElUsoEsElDelMesArgentino();
        thenSeBloqueaElNegocioAntesDeLeerLaSuscripcion();
    }

    @Test
    @DisplayName("La prueba usa sus diez páginas totales y fechas originales aunque cruce de mes")
    void executeShouldUseTrialTotalInsteadOfMonthlyUsage() {
        givenUnaPruebaActiva();

        whenSeConsulta(AHORA);

        thenElUsoEsElDeLaPrueba();
        thenNoSeConsultaElUsoMensualDeIa();
    }

    @Test
    @DisplayName("Premium vigente sin historial no ofrece una prueba ni altera sus cien páginas mensuales")
    void executeShouldKeepPaidPremiumAndDisallowTrial() {
        givenUnPremiumPagoVigente();

        whenSeConsulta(AHORA);

        thenSeMuestraPremiumConSusLimitesMensuales();
    }

    @Test
    @DisplayName("Al vencer la prueba muestra gratuito y conserva el historial sin esperar al job")
    void executeShouldFallBackAtExactTrialExpiry() {
        givenUnaPruebaQueVenceAhora();

        whenSeConsulta(AHORA);

        thenSeMuestraElGratuitoConElHistorialDeLaPrueba();
    }

    @Test
    @DisplayName("Una suscripción futura usa el gratuito y conserva la disponibilidad de prueba no usada")
    void executeShouldTreatFutureSubscriptionAsFree() {
        givenUnaSuscripcionFutura();

        whenSeConsulta(AHORA);

        thenSeMuestraElGratuitoConPruebaDisponible();
    }

    @Test
    @DisplayName("No ofrece prueba si las capacidades premium aún no están configuradas")
    void executeShouldNotOfferUnconfiguredTrial() {
        givenUnPremiumSinLimitesConfigurados();

        whenSeConsulta(AHORA);

        thenNoSeOfreceLaPrueba();
    }

    @Test
    @DisplayName("Un catálogo ausente falla sin consultar datos privados")
    void executeShouldRejectMissingCatalog() {
        whenSeConsultaConError(AHORA);

        thenFallaPorRecursoInexistente("Plan no encontrado");
        thenNoSeConsultaLaSuscripcion();
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void executeShouldRejectMissingSubscription() {
        givenUnCatalogoSinSuscripcion();

        whenSeConsultaConError(AHORA);

        thenFallaPorRecursoInexistente("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una referencia a un plan inexistente no se presenta como premium")
    void executeShouldRejectMissingAssignedPlan() {
        givenUnaSuscripcionConPlanInexistente();

        whenSeConsultaConError(AHORA);

        thenFallaPorRecursoInexistente("Plan no encontrado");
    }

    @Test
    @DisplayName("Una prueba activa sin historial no devuelve capacidades incompletas")
    void executeShouldRejectMissingTrialHistory() {
        givenUnaPruebaActivaSinHistorial();

        whenSeConsultaConError(AHORA);

        thenFallaPorRecursoInexistente("Prueba no encontrada");
    }

    @Test
    @DisplayName("Un plan efectivo sin límites configurados falla de forma cerrada")
    void executeShouldRejectUnconfiguredEffectivePlan() {
        givenUnGratuitoSinLimitesConfigurados();

        whenSeConsultaConError(AHORA);

        thenSeRechazaPorRestriccionDePlan();
    }

    @Test
    @DisplayName("Un usuario sin negocio no consulta planes de otro usuario")
    void executeShouldPropagateMissingBusiness() {
        givenUnUsuarioSinNegocio();

        whenSeConsultaConError(AHORA);

        thenFallaPorRecursoInexistente("Negocio no encontrado");
        thenNoSeConsultaNingunDatoDelPlan();
    }

    // --- arrange ---
    private void givenUnGratuitoConUsoDelMes() {
        Plan gratuito = givenCatalogo();
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
        when(usoPlanRepository.contarProyectos(NEGOCIO_ID)).thenReturn(2L);
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(1001L);
        when(usoPlanRepository.paginasEnPeriodo(NEGOCIO_ID, INICIO_DEL_MES, INICIO_DEL_MES.plusMonths(1)))
                .thenReturn(7L);
        when(usoPlanRepository.objetivosIaEnPeriodo(
                NEGOCIO_ID, INICIO_DEL_MES_EN_UTC, INICIO_DEL_MES_SIGUIENTE_EN_UTC)).thenReturn(3L);
    }

    private void givenUnaPruebaActiva() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void givenUnPremiumPagoVigente() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void givenUnaPruebaQueVenceAhora() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA);
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));
    }

    private void givenUnaSuscripcionFutura() {
        givenCatalogo();
        Suscripcion suscripcion = givenSuscripcion(premium(), EstadoSuscripcion.CANCELADA, AHORA.plusDays(30));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioEn(AHORA.plusDays(1));
    }

    private void givenUnPremiumSinLimitesConfigurados() {
        Plan gratuito = plan();
        Plan premium = premium();
        premium.setLimitesConfigurados(false);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void givenUnCatalogoSinSuscripcion() {
        givenCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenUnaSuscripcionConPlanInexistente() {
        givenCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));
    }

    private void givenUnaPruebaActivaSinHistorial() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
    }

    private void givenUnGratuitoSinLimitesConfigurados() {
        Plan gratuito = givenCatalogo();
        gratuito.setLimitesConfigurados(false);
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
    }

    private void givenUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    private Plan givenCatalogo() {
        Plan gratuito = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        return gratuito;
    }

    private Suscripcion givenSuscripcion(Plan plan, EstadoSuscripcion estado, OffsetDateTime fin) {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setPlanId(plan.getId());
        suscripcion.setEstado(estado);
        suscripcion.setInicioEn(AHORA.minusDays(3));
        suscripcion.setFinEn(fin);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        if (suscripcion.estaVigente(AHORA)) {
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        }
        return suscripcion;
    }

    // --- act ---
    private void whenSeConsulta(OffsetDateTime ahora) {
        respuesta = new ConsultarPlanes(negocioRepository, suscripcionRepository,
                planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(USUARIO_ID);
    }

    private void whenSeConsultaConError(OffsetDateTime ahora) {
        error = catchThrowable(() -> whenSeConsulta(ahora));
    }

    // --- assert ---
    private void thenSeMuestraElGratuitoConSuCatalogoYSuPrueba() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.planes()).extracting(PlanItem::codigo).containsExactly("GRATUITO", "PREMIUM");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isTrue();
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.prueba()).isNull();
    }

    private void thenElUsoEsElDelMesArgentino() {
        assertThat(respuesta.uso()).isEqualTo(new UsoPlanItem(2, 1001, 7, 10, "MENSUAL",
                INICIO_DEL_MES_EN_UTC, INICIO_DEL_MES_SIGUIENTE_EN_UTC, 3, 5));
    }

    private void thenSeBloqueaElNegocioAntesDeLeerLaSuscripcion() {
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
    }

    private void thenElUsoEsElDeLaPrueba() {
        assertThat(respuesta.suscripcion().pruebaActiva()).isTrue();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.uso().paginasIa()).isEqualTo(8);
        assertThat(respuesta.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(respuesta.uso().periodoIa()).isEqualTo("PRUEBA");
        assertThat(respuesta.uso().limiteObjetivosIa()).isEqualTo(20);
        assertThat(respuesta.uso().inicioPeriodoIa()).isEqualTo(prueba().inicioEn());
        assertThat(respuesta.uso().finPeriodoIa()).isEqualTo(prueba().finEn());
    }

    private void thenNoSeConsultaElUsoMensualDeIa() {
        verify(usoPlanRepository, never()).paginasEnPeriodo(NEGOCIO_ID, INICIO_DEL_MES, INICIO_DEL_MES.plusMonths(1));
    }

    private void thenSeMuestraPremiumConSusLimitesMensuales() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("PREMIUM");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.uso().limitePaginasIa()).isEqualTo(100);
        assertThat(respuesta.uso().limiteObjetivosIa()).isEqualTo(20);
        assertThat(respuesta.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void thenSeMuestraElGratuitoConElHistorialDeLaPrueba() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.suscripcion().pruebaActiva()).isFalse();
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(respuesta.prueba()).isEqualTo(PruebaPremiumResponse.de(prueba()));
        assertThat(respuesta.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    private void thenSeMuestraElGratuitoConPruebaDisponible() {
        assertThat(respuesta.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    private void thenNoSeOfreceLaPrueba() {
        assertThat(respuesta.suscripcion().puedeIniciarPrueba()).isFalse();
    }

    private void thenFallaPorRecursoInexistente(String mensaje) {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
    }

    private void thenSeRechazaPorRestriccionDePlan() {
        assertThat(error).isInstanceOf(RestriccionPlanException.class);
    }

    private void thenNoSeConsultaLaSuscripcion() {
        verifyNoInteractions(suscripcionRepository);
    }

    private void thenNoSeConsultaNingunDatoDelPlan() {
        verifyNoInteractions(planRepository, suscripcionRepository, usoPlanRepository);
    }
}
