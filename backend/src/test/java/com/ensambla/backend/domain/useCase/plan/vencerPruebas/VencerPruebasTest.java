package com.ensambla.backend.domain.useCase.plan.vencerPruebas;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VencerPruebasTest {

    private final PlanRepository planRepository = mock(PlanRepository.class, CALLS_REAL_METHODS);
    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);
    private final PruebaPremiumRepository pruebaPremiumRepository = mock(PruebaPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);

    @Test
    @DisplayName("El vencimiento devuelve el gratuito con el reloj del servidor sin borrar el historial de prueba")
    void executeShouldRestoreFreePlanAtExactEnd() {
        Suscripcion suscripcion = givenPruebaQueTermina(INSTANTE);
        Plan gratuito = plan();
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(gratuito));

        vencerA(INSTANTE);

        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcion.getPlanId()).isEqualTo(gratuito.getId());
        assertThat(suscripcion.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(suscripcion.getFinEn()).isNull();
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(INSTANTE);
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        verify(suscripcionRepository).save(suscripcion);
        verify(pruebaPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("El proceso no vence una prueba antes de su fecha de fin")
    void executeShouldKeepUnexpiredTrial() {
        givenPruebaQueTermina(INSTANTE.plusDays(1));

        vencerA(INSTANTE);

        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("Un pago activado después de listar candidatos no es degradado por el job")
    void executeShouldPreserveSubscriptionActivatedAfterSelection() {
        givenCandidato();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion()));

        vencerA(INSTANTE.plusDays(30));

        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("Un lote sin pruebas vencidas no modifica suscripciones")
    void executeShouldSkipEmptyBatch() {
        when(pruebaPremiumRepository.findNegociosVencidos(INSTANTE)).thenReturn(List.of());

        vencerA(INSTANTE);

        verifyNoInteractions(suscripcionRepository, usoPlanRepository);
    }

    // --- arrange ---
    private Suscripcion givenPruebaQueTermina(OffsetDateTime fin) {
        givenCandidato();
        Suscripcion suscripcion = suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setFinEn(fin);
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
        return suscripcion;
    }

    private void givenCandidato() {
        when(pruebaPremiumRepository.findNegociosVencidos(any())).thenReturn(List.of(NEGOCIO_ID));
    }

    // --- act ---
    private void vencerA(OffsetDateTime ahora) {
        new VencerPruebas(planRepository, pruebaPremiumRepository,
                suscripcionRepository, usoPlanRepository, new TransaccionDirecta(),
                Clock.fixed(ahora.toInstant(), ZoneOffset.UTC))
                .execute();
    }
}
