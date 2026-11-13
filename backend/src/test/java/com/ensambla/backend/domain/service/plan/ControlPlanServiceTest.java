package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.PLAN_ID;
import static com.ensambla.backend.support.PlanTestData.USUARIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Mock
    private PruebaPremiumRepository pruebaPremiumRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @Mock
    private NegocioRepository negocioRepository;

    @Spy
    private AccesoIa accesoIa = AccesoIa.LIBRE;

    @InjectMocks
    private ControlPlanService controlPlanService;

    @BeforeEach
    void givenNegocioDeLaCuenta() {
        lenient().when(negocioRepository.obtenerDuenioId(NEGOCIO_ID)).thenReturn(USUARIO_ID);
    }

    @Test
    @DisplayName("Bloquea la cuenta antes de contar y autorizar un negocio")
    void validarAltaNegocioShouldLockBeforeCounting() {
        givenActivePlan();

        controlPlanService.validarAltaNegocio(USUARIO_ID, INSTANTE);

        var orden = inOrder(usoPlanRepository, suscripcionRepository);
        orden.verify(usoPlanRepository).bloquearCuenta(USUARIO_ID);
        orden.verify(suscripcionRepository).findByUsuarioId(USUARIO_ID);
        orden.verify(usoPlanRepository).contarNegocios(USUARIO_ID);
    }

    @Test
    @DisplayName("Rechaza un negocio más cuando la cuenta ya alcanzó el límite de su plan")
    void validarAltaNegocioShouldRejectWhenLimitReached() {
        givenActivePlan();
        when(usoPlanRepository.contarNegocios(USUARIO_ID)).thenReturn(3L);

        assertThatThrownBy(() -> controlPlanService.validarAltaNegocio(USUARIO_ID, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class)
                .extracting("codigo").isEqualTo("LIMITE_NEGOCIOS");
    }

    @Test
    @DisplayName("Rechaza todas las filas del lote cuando no entran en la cuota")
    void validarAltaRegistrosShouldRejectWholeBatch() {
        givenActivePlan();
        when(usoPlanRepository.contarRegistros(NEGOCIO_ID)).thenReturn(99L);

        assertThatThrownBy(() -> controlPlanService.validarAltaRegistros(NEGOCIO_ID, 2, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class);
    }

    @Test
    @DisplayName("Calcula el mes por el día argentino aunque en UTC ya sea el mes siguiente")
    void validarPaginasIaShouldUseArgentinaMonth() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setInicioEn(INSTANTE.minusMonths(1));
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        OffsetDateTime ahora = OffsetDateTime.parse("2026-10-01T02:30:00Z");

        controlPlanService.validarPaginasIa(NEGOCIO_ID, 1, ahora);

        verify(usoPlanRepository).paginasEnPeriodo(USUARIO_ID,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Una función incluida se permite sin bloquear ni contar las altas")
    void validarFuncionShouldAllowEnabledFeature() {
        Plan plan = givenActivePlan();
        plan.setFunciones(List.of(FuncionPlan.RECETAS_COSTOS.name()));

        assertThatCode(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE)).doesNotThrowAnyException();

        verifyNoInteractions(usoPlanRepository);
    }

    @Test
    @DisplayName("La suscripción vencida usa el gratuito aunque todavía figure activa en persistencia")
    void validarFuncionShouldFallBackAtExactExpiry() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setFinEn(INSTANTE);
        givenFallback(suscripcion);

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class).hasMessageContaining("premium activo");
    }

    @Test
    @DisplayName("Una suscripción cancelada conserva los límites de alta del gratuito")
    void validarAltaNegocioShouldUseFreePlanWhenCancelled() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        givenFallback(suscripcion);

        controlPlanService.validarAltaNegocio(USUARIO_ID, INSTANTE);

        verify(planRepository).findByCodigo(CodigoPlan.GRATUITO);
    }

    @Test
    @DisplayName("Una cuenta sin suscripción no recibe capacidades por defecto")
    void validarFuncionShouldRejectMissingSubscription() {
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");

        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("No habilita un plan referido que ya no existe")
    void validarFuncionShouldRejectMissingAssignedPlan() {
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");
    }

    @Test
    @DisplayName("Una suscripción vencida no habilita funciones si falta el catálogo gratuito")
    void validarFuncionShouldRejectMissingFallbackPlan() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.VENCIDA);
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controlPlanService.validarFuncion(
                NEGOCIO_ID, FuncionPlan.RECETAS_COSTOS, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan gratuito no encontrado");
    }

    @Test
    @DisplayName("Una prueba sin historial persistido falla sin autorizar páginas")
    void validarPaginasIaShouldRejectTrialWithoutHistory() {
        givenActivePlan();
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(prueba));

        assertThatThrownBy(() -> controlPlanService.validarPaginasIa(NEGOCIO_ID, 1, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Prueba no encontrada");
    }

    @Test
    @DisplayName("Una foto inmediata valida el mes, reserva en el día argentino con el límite diario y no lanza si entra")
    void reservarPaginaIaInmediataShouldReserveInTheArgentinaDay() {
        givenActivePlan();
        OffsetDateTime enArgentina = INSTANTE.withOffsetSameInstant(ZoneOffset.ofHours(-3));
        LocalDate diaArgentino = ZonaNegocio.dia(INSTANTE);
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, diaArgentino, 100, enArgentina)).thenReturn(true);

        controlPlanService.reservarPaginaIaInmediata(NEGOCIO_ID, 100, enArgentina);

        verify(usoPlanRepository).registrarPaginaInmediata(NEGOCIO_ID, diaArgentino, 100, enArgentina);
    }

    @Test
    @DisplayName("Una foto inmediata con el límite diario agotado responde la restricción del día")
    void reservarPaginaIaInmediataShouldRejectWhenDailyLimitIsReached() {
        givenActivePlan();
        when(usoPlanRepository.registrarPaginaInmediata(any(), any(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> controlPlanService.reservarPaginaIaInmediata(NEGOCIO_ID, 100, INSTANTE))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
    }

    @Test
    @DisplayName("La reserva de páginas tampoco admite una cuenta sin suscripción")
    void reservarPaginasIaShouldRejectMissingSubscription() {
        assertThatThrownBy(() -> controlPlanService.reservarPaginasIa(NEGOCIO_ID, 1, INSTANTE, () -> true))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
    }

    @Test
    @DisplayName("Una prueba vencida usa la cuota mensual gratuita y no incrementa el historial de prueba")
    void reservarPaginasIaShouldIgnoreExpiredTrial() {
        Suscripcion prueba = suscripcion();
        prueba.setEstado(EstadoSuscripcion.PRUEBA);
        prueba.setFinEn(INSTANTE);
        givenFallback(prueba);

        boolean result = controlPlanService.reservarPaginasIa(NEGOCIO_ID, 1, INSTANTE, () -> true);

        assertThat(result).isTrue();

        verifyNoInteractions(pruebaPremiumRepository);
    }

    @Test
    @DisplayName("Valida la cuota de la prueba antes de la reserva diaria y, si entra, la descuenta de la prueba")
    void reservarPaginasIaShouldValidateReserveAndChargeTrialInOrder() {
        PruebaPremium prueba = givenPruebaVigente();
        BooleanSupplier reserva = mock(BooleanSupplier.class);
        when(reserva.getAsBoolean()).thenReturn(true);

        boolean result = controlPlanService.reservarPaginasIa(NEGOCIO_ID, 2, INSTANTE, reserva);

        assertThat(result).isTrue();
        var orden = inOrder(usoPlanRepository, reserva, pruebaPremiumRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(reserva).getAsBoolean();
        orden.verify(pruebaPremiumRepository).save(prueba.consumir(2));
    }

    @Test
    @DisplayName("Una reserva diaria que no entra no descuenta páginas de la prueba")
    void reservarPaginasIaShouldNotChargeTrialWhenReservationFails() {
        givenPruebaVigente();

        boolean result = controlPlanService.reservarPaginasIa(NEGOCIO_ID, 2, INSTANTE, () -> false);

        assertThat(result).isFalse();
        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un rechazo de la cuota mensual no llega a hacer la reserva diaria")
    void reservarPaginasIaShouldNotReserveWhenMonthlyQuotaIsFull() {
        givenActivePlan();
        when(usoPlanRepository.paginasEnPeriodo(any(), any(), any())).thenReturn(10L);
        BooleanSupplier reserva = mock(BooleanSupplier.class);

        assertThatThrownBy(() -> controlPlanService.reservarPaginasIa(NEGOCIO_ID, 1, INSTANTE, reserva))
                .isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(reserva);
    }

    // --- arrange ---
    private Plan givenActivePlan() {
        Plan plan = plan();
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        return plan;
    }

    private PruebaPremium givenPruebaVigente() {
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        PruebaPremium prueba = PruebaPremium.nueva(USUARIO_ID, INSTANTE);
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        when(pruebaPremiumRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(prueba));
        return prueba;
    }

    private void givenFallback(Suscripcion suscripcion) {
        when(suscripcionRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
    }
}
