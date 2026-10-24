package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.ConsultaPlanesTestData.prueba;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaPlanesServiceTest {

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionService suscripcionService;

    @Mock
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @InjectMocks
    private ConsultaPlanesService consultaPlanesService;

    @Test
    @DisplayName("El gratuito muestra catálogo, uso mensual UTC y disponibilidad de prueba bajo el mismo bloqueo")
    void consultarShouldReturnFreePlanAndUtcMonth() {
        Plan gratuito = givenCatalogo();
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
        LocalDate inicio = LocalDate.parse("2026-11-01");
        when(usoPlanRepository.contarProyectos(NEGOCIO_ID)).thenReturn(2L);
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(1001L);
        when(usoPlanRepository.paginasEnPeriodo(NEGOCIO_ID, inicio, inicio.plusMonths(1))).thenReturn(7L);

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA.withOffsetSameInstant(
                ZoneOffset.ofHours(-3)));

        assertThat(result.planActual().getCodigo()).isEqualTo(CodigoPlan.GRATUITO);
        assertThat(result.planes()).extracting(Plan::getCodigo)
                .containsExactly(CodigoPlan.GRATUITO, CodigoPlan.PREMIUM);
        assertThat(result.puedeIniciarPrueba()).isTrue();
        assertThat(result.pruebaActiva()).isFalse();
        assertThat(result.prueba()).isNull();
        assertThat(result.proyectos()).isEqualTo(2);
        assertThat(result.registros()).isEqualTo(1001);
        assertThat(result.paginasIa()).isEqualTo(7);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
        assertThat(result.periodoIa()).isEqualTo("MENSUAL");
        assertThat(result.inicioPeriodoIa()).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00:00Z"));
        assertThat(result.finPeriodoIa()).isEqualTo(OffsetDateTime.parse("2026-12-01T00:00:00Z"));
        var orden = inOrder(usoPlanRepository, suscripcionService);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionService).obtener(NEGOCIO_ID);
    }

    @Test
    @DisplayName("La prueba usa sus diez páginas totales y fechas originales aunque cruce de mes")
    void consultarShouldUseTrialTotalInsteadOfMonthlyUsage() {
        givenCatalogo();
        Plan premium = premium();
        givenSuscripcion(premium, EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA);

        assertThat(result.pruebaActiva()).isTrue();
        assertThat(result.puedeIniciarPrueba()).isFalse();
        assertThat(result.paginasIa()).isEqualTo(8);
        assertThat(result.limitePaginasIa()).isEqualTo(10);
        assertThat(result.periodoIa()).isEqualTo("PRUEBA");
        assertThat(result.inicioPeriodoIa()).isEqualTo(prueba().inicioEn());
        assertThat(result.finPeriodoIa()).isEqualTo(prueba().finEn());
        verify(usoPlanRepository, never()).paginasEnPeriodo(NEGOCIO_ID,
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01"));
    }

    @Test
    @DisplayName("Premium vigente sin historial no ofrece una prueba ni altera sus cien páginas mensuales")
    void consultarShouldKeepPaidPremiumAndDisallowTrial() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA);

        assertThat(result.planActual().getCodigo()).isEqualTo(CodigoPlan.PREMIUM);
        assertThat(result.puedeIniciarPrueba()).isFalse();
        assertThat(result.pruebaActiva()).isFalse();
        assertThat(result.limitePaginasIa()).isEqualTo(100);
        assertThat(result.periodoIa()).isEqualTo("MENSUAL");
    }

    @Test
    @DisplayName("Al vencer la prueba muestra gratuito y conserva el historial sin esperar al job")
    void consultarShouldFallBackAtExactTrialExpiry() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA);
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA);

        assertThat(result.planActual().getCodigo()).isEqualTo(CodigoPlan.GRATUITO);
        assertThat(result.pruebaActiva()).isFalse();
        assertThat(result.puedeIniciarPrueba()).isFalse();
        assertThat(result.prueba()).isEqualTo(prueba());
        assertThat(result.periodoIa()).isEqualTo("MENSUAL");
    }

    @Test
    @DisplayName("Una suscripción futura usa el gratuito y conserva la disponibilidad de prueba no usada")
    void consultarShouldTreatFutureSubscriptionAsFree() {
        givenCatalogo();
        Suscripcion suscripcion = givenSuscripcion(premium(), EstadoSuscripcion.CANCELADA, AHORA.plusDays(30));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioEn(AHORA.plusDays(1));

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA);

        assertThat(result.planActual().getCodigo()).isEqualTo(CodigoPlan.GRATUITO);
        assertThat(result.puedeIniciarPrueba()).isTrue();
    }

    @Test
    @DisplayName("No ofrece prueba si las capacidades premium aún no están configuradas")
    void consultarShouldNotOfferUnconfiguredTrial() {
        Plan gratuito = plan();
        Plan premium = premium();
        premium.setLimitesConfigurados(false);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        ConsultaPlanes result = consultaPlanesService.consultar(NEGOCIO_ID, AHORA);

        assertThat(result.puedeIniciarPrueba()).isFalse();
    }

    @Test
    @DisplayName("Un catálogo ausente falla sin consultar datos privados")
    void consultarShouldRejectMissingCatalog() {
        assertThatThrownBy(() -> consultaPlanesService.consultar(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");

        verifyNoInteractions(suscripcionService);
    }

    @Test
    @DisplayName("Una referencia a un plan inexistente no se presenta como premium")
    void consultarShouldRejectMissingAssignedPlan() {
        givenCatalogo();
        when(suscripcionService.obtener(NEGOCIO_ID)).thenReturn(suscripcion());

        assertThatThrownBy(() -> consultaPlanesService.consultar(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }

    @Test
    @DisplayName("Una prueba activa sin historial no devuelve capacidades incompletas")
    void consultarShouldRejectMissingTrialHistory() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));

        assertThatThrownBy(() -> consultaPlanesService.consultar(NEGOCIO_ID, AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Prueba no encontrada");
    }

    @Test
    @DisplayName("Un plan efectivo sin límites configurados falla de forma cerrada")
    void consultarShouldRejectUnconfiguredEffectivePlan() {
        Plan gratuito = givenCatalogo();
        gratuito.setLimitesConfigurados(false);
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        assertThatThrownBy(() -> consultaPlanesService.consultar(NEGOCIO_ID, AHORA))
                .isInstanceOf(RestriccionPlanException.class);
    }

    // --- arrange ---
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
        when(suscripcionService.obtener(NEGOCIO_ID)).thenReturn(suscripcion);
        if (suscripcion.estaVigente(AHORA)) {
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        }
        return suscripcion;
    }
}
