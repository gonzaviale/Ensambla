package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IniciarPruebaTest {

    private static final UUID USUARIO_ID = UUID.fromString("bb000000-0000-4000-8000-000000000009");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);
    private final IniciarPrueba iniciarPrueba = new IniciarPrueba(negocioService,
            new SuscripcionService(planRepository, suscripcionRepository), pruebaPremiumRepository,
            suscripcionRepository, usoPlanRepository, renovacionPremiumRepository, new TransaccionDirecta(),
            Clock.fixed(INSTANTE.toInstant(), ZoneOffset.UTC));

    @BeforeEach
    void givenNegocioDelUsuario() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Bloquea el negocio y activa siete días premium con el reloj del servidor")
    void executeShouldActivateTrialUnderBusinessLock() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        givenSaveDevuelveLaPrueba();

        PruebaPremiumResponse result = iniciar();

        assertThat(result).isEqualTo(PruebaPremiumResponse.de(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE)));
        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.PRUEBA);
        assertThat(suscripcion.getPlanId()).isEqualTo(premium.getId());
        assertThat(suscripcion.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(suscripcion.getFinEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(INSTANTE);
        var orden = inOrder(usoPlanRepository, suscripcionRepository, pruebaPremiumRepository);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(pruebaPremiumRepository).findByNegocioId(NEGOCIO_ID);
        orden.verify(pruebaPremiumRepository).save(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE));
        orden.verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    @DisplayName("Un negocio no puede reiniciar una prueba usada aunque su suscripción cambie")
    void executeShouldRejectUsedTrial() {
        givenSuscripcion();
        when(pruebaPremiumRepository.findByNegocioId(NEGOCIO_ID))
                .thenReturn(Optional.of(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE)));

        assertThatThrownBy(this::iniciar)
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PRUEBA_UTILIZADA");

        verifyNoInteractions(planRepository);
        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("No inicia la prueba con una contratación premium en curso")
    void executeShouldRejectOpenRenewal() {
        givenSuscripcion();
        when(renovacionPremiumRepository.existeAbierta(NEGOCIO_ID)).thenReturn(true);

        assertThatThrownBy(this::iniciar)
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("RENOVACION_EN_CURSO");

        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un premium pagado vigente no es reemplazado por una prueba")
    void executeShouldRejectActivePremium() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        suscripcion.setPlanId(premium.getId());

        assertThatThrownBy(this::iniciar)
                .isInstanceOf(RestriccionPlanException.class).extracting("codigo").isEqualTo("PREMIUM_ACTIVO");
    }

    @Test
    @DisplayName("Un premium vencido que nunca usó prueba puede iniciarla")
    void executeShouldAllowExpiredSubscription() {
        Suscripcion suscripcion = givenSuscripcion();
        Plan premium = givenPremium();
        suscripcion.setPlanId(premium.getId());
        suscripcion.setFinEn(INSTANTE);
        givenSaveDevuelveLaPrueba();

        PruebaPremiumResponse result = iniciar();

        assertThat(result.paginasIaUtilizadas()).isZero();
    }

    @Test
    @DisplayName("Falla sin suscripción y no consulta el catálogo")
    void executeShouldRejectMissingSubscription() {
        assertThatThrownBy(this::iniciar)
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");

        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("No inicia la prueba si falta premium en el catálogo")
    void executeShouldRejectMissingPremium() {
        givenSuscripcion();

        assertThatThrownBy(this::iniciar)
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Plan no encontrado");

        verify(pruebaPremiumRepository, never()).save(any());
    }

    // --- arrange ---
    private Suscripcion givenSuscripcion() {
        Suscripcion suscripcion = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        return suscripcion;
    }

    private Plan givenPremium() {
        Plan premium = plan();
        premium.setId(UUID.randomUUID());
        premium.setCodigo(CodigoPlan.PREMIUM);
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium));
        return premium;
    }

    private void givenSaveDevuelveLaPrueba() {
        when(pruebaPremiumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- act ---
    private PruebaPremiumResponse iniciar() {
        return iniciarPrueba.execute(new IniciarPruebaRequest(USUARIO_ID));
    }
}
