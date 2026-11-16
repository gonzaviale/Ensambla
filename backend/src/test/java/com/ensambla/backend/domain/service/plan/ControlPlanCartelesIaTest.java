package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.PLAN_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPlanCartelesIaTest {

    private static final LocalDate NOVIEMBRE = LocalDate.parse("2026-11-01");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-16T12:00:00Z");

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

    @Test
    @DisplayName("Con cupo en el mes argentino, el cartel con IA se admite")
    void exigirCupoDeCartelesIaShouldAllowWithQuotaLeft() {
        givenUnPlanConLimiteYUsados(10, 3);

        controlPlanService.exigirCupoDeCartelesIa(NEGOCIO_ID, AHORA);

        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        verify(usoPlanRepository).cartelesIaEnElMes(NEGOCIO_ID, NOVIEMBRE);
    }

    @Test
    @DisplayName("Sin cupo, el cartel con IA se rechaza con el código del cupo de carteles")
    void exigirCupoDeCartelesIaShouldRejectWhenTheMonthIsFull() {
        givenUnPlanConLimiteYUsados(10, 10);

        assertThatThrownBy(() -> controlPlanService.exigirCupoDeCartelesIa(NEGOCIO_ID, AHORA))
                .isInstanceOf(RestriccionPlanException.class);
        verify(usoPlanRepository, never()).registrarCartelIa(any(), any(), any());
    }

    @Test
    @DisplayName("Registrar un cartel con IA lo suma al mes argentino si todavía hay cupo")
    void registrarCartelIaShouldCountItInTheMonth() {
        givenUnPlanConLimiteYUsados(10, 3);

        controlPlanService.registrarCartelIa(NEGOCIO_ID, AHORA);

        verify(usoPlanRepository).registrarCartelIa(NEGOCIO_ID, NOVIEMBRE, AHORA);
    }

    @Test
    @DisplayName("Registrar un cartel con IA sin cupo lo rechaza y no suma nada")
    void registrarCartelIaShouldRejectWhenTheMonthIsFull() {
        givenUnPlanConLimiteYUsados(10, 10);

        assertThatThrownBy(() -> controlPlanService.registrarCartelIa(NEGOCIO_ID, AHORA))
                .isInstanceOf(RestriccionPlanException.class);
        verify(usoPlanRepository, never()).registrarCartelIa(any(), any(), any());
    }

    private void givenUnPlanConLimiteYUsados(int limite, long usados) {
        Plan plan = plan();
        plan.setLimiteCartelesIaMensuales(limite);
        var suscripcion = suscripcion();
        suscripcion.setInicioEn(INSTANTE.minusMonths(1));
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(usoPlanRepository.cartelesIaEnElMes(NEGOCIO_ID, NOVIEMBRE)).thenReturn(usados);
    }
}
