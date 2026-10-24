package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ConsultaPlanesTestData.AHORA;
import static com.ensambla.backend.support.ConsultaPlanesTestData.premium;
import static com.ensambla.backend.support.ConsultaPlanesTestData.prueba;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarPlanesTest {

    private static final UUID USUARIO_ID = UUID.fromString("bb000000-0000-4000-8000-000000000009");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);

    @BeforeEach
    void givenNegocioDelUsuario() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El gratuito muestra catálogo, uso mensual UTC y disponibilidad de prueba bajo el mismo bloqueo")
    void executeShouldReturnFreePlanAndUtcMonth() {
        Plan gratuito = givenCatalogo();
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));
        LocalDate inicio = LocalDate.parse("2026-11-01");
        when(usoPlanRepository.contarProyectos(NEGOCIO_ID)).thenReturn(2L);
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(1001L);
        when(usoPlanRepository.paginasEnPeriodo(NEGOCIO_ID, inicio, inicio.plusMonths(1))).thenReturn(7L);

        ConsultarPlanesResponse result = consultar(AHORA.withOffsetSameInstant(ZoneOffset.ofHours(-3)));

        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.planes()).extracting(PlanItem::codigo).containsExactly("GRATUITO", "PREMIUM");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isTrue();
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.prueba()).isNull();
        assertThat(result.uso()).isEqualTo(new UsoPlanItem(2, 1001, 7, 10, "MENSUAL",
                OffsetDateTime.parse("2026-11-01T00:00:00Z"), OffsetDateTime.parse("2026-12-01T00:00:00Z")));
        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("La prueba usa sus diez páginas totales y fechas originales aunque cruce de mes")
    void executeShouldUseTrialTotalInsteadOfMonthlyUsage() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));

        ConsultarPlanesResponse result = consultar(AHORA);

        assertThat(result.suscripcion().pruebaActiva()).isTrue();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.uso().paginasIa()).isEqualTo(8);
        assertThat(result.uso().limitePaginasIa()).isEqualTo(10);
        assertThat(result.uso().periodoIa()).isEqualTo("PRUEBA");
        assertThat(result.uso().inicioPeriodoIa()).isEqualTo(prueba().inicioEn());
        assertThat(result.uso().finPeriodoIa()).isEqualTo(prueba().finEn());
        verify(usoPlanRepository, never()).paginasEnPeriodo(NEGOCIO_ID,
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01"));
    }

    @Test
    @DisplayName("Premium vigente sin historial no ofrece una prueba ni altera sus cien páginas mensuales")
    void executeShouldKeepPaidPremiumAndDisallowTrial() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        ConsultarPlanesResponse result = consultar(AHORA);

        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("PREMIUM");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.uso().limitePaginasIa()).isEqualTo(100);
        assertThat(result.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    @Test
    @DisplayName("Al vencer la prueba muestra gratuito y conserva el historial sin esperar al job")
    void executeShouldFallBackAtExactTrialExpiry() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA);
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(prueba()));

        ConsultarPlanesResponse result = consultar(AHORA);

        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.suscripcion().pruebaActiva()).isFalse();
        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
        assertThat(result.prueba()).isEqualTo(PruebaPremiumResponse.de(prueba()));
        assertThat(result.uso().periodoIa()).isEqualTo("MENSUAL");
    }

    @Test
    @DisplayName("Una suscripción futura usa el gratuito y conserva la disponibilidad de prueba no usada")
    void executeShouldTreatFutureSubscriptionAsFree() {
        givenCatalogo();
        Suscripcion suscripcion = givenSuscripcion(premium(), EstadoSuscripcion.CANCELADA, AHORA.plusDays(30));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioEn(AHORA.plusDays(1));

        ConsultarPlanesResponse result = consultar(AHORA);

        assertThat(result.suscripcion().codigoPlanActual()).isEqualTo("GRATUITO");
        assertThat(result.suscripcion().puedeIniciarPrueba()).isTrue();
    }

    @Test
    @DisplayName("No ofrece prueba si las capacidades premium aún no están configuradas")
    void executeShouldNotOfferUnconfiguredTrial() {
        Plan gratuito = plan();
        Plan premium = premium();
        premium.setLimitesConfigurados(false);
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        ConsultarPlanesResponse result = consultar(AHORA);

        assertThat(result.suscripcion().puedeIniciarPrueba()).isFalse();
    }

    @Test
    @DisplayName("Un catálogo ausente falla sin consultar datos privados")
    void executeShouldRejectMissingCatalog() {
        assertThatThrownBy(() -> consultar(AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");

        verifyNoInteractions(suscripcionRepository);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void executeShouldRejectMissingSubscription() {
        givenCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar(AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una referencia a un plan inexistente no se presenta como premium")
    void executeShouldRejectMissingAssignedPlan() {
        givenCatalogo();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));

        assertThatThrownBy(() -> consultar(AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }

    @Test
    @DisplayName("Una prueba activa sin historial no devuelve capacidades incompletas")
    void executeShouldRejectMissingTrialHistory() {
        givenCatalogo();
        givenSuscripcion(premium(), EstadoSuscripcion.PRUEBA, AHORA.plusDays(4));

        assertThatThrownBy(() -> consultar(AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Prueba no encontrada");
    }

    @Test
    @DisplayName("Un plan efectivo sin límites configurados falla de forma cerrada")
    void executeShouldRejectUnconfiguredEffectivePlan() {
        Plan gratuito = givenCatalogo();
        gratuito.setLimitesConfigurados(false);
        givenSuscripcion(gratuito, EstadoSuscripcion.ACTIVA, AHORA.plusDays(30));

        assertThatThrownBy(() -> consultar(AHORA)).isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("Un usuario sin negocio no consulta planes de otro usuario")
    void executeShouldPropagateMissingBusiness() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));

        assertThatThrownBy(() -> consultar(AHORA))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Negocio no encontrado");

        verifyNoInteractions(planRepository, suscripcionRepository, usoPlanRepository);
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
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        if (suscripcion.estaVigente(AHORA)) {
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        }
        return suscripcion;
    }

    // --- act ---
    private ConsultarPlanesResponse consultar(OffsetDateTime ahora) {
        return new ConsultarPlanes(negocioService, new SuscripcionService(planRepository, suscripcionRepository),
                planRepository, pruebaPremiumRepository,
                usoPlanRepository, renovacionPremiumRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(USUARIO_ID);
    }
}
