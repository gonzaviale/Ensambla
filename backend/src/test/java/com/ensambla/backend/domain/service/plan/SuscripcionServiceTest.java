package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.PLAN_ID;
import static com.ensambla.backend.support.PlanTestData.plan;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @InjectMocks
    private SuscripcionService suscripcionService;

    @Test
    @DisplayName("Asigna el gratuito activo con las fechas del instante recibido, sin fecha de fin")
    void asignarGratuitoShouldPersistActiveSubscription() {
        givenFreePlanAvailable();

        Suscripcion result = suscripcionService.asignarGratuito(NEGOCIO_ID, INSTANTE);

        thenFreeSubscriptionSaved(result);
    }

    @Test
    @DisplayName("No crea una suscripción si falta el plan gratuito")
    void asignarGratuitoShouldRejectMissingFreePlan() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.asignarGratuito(NEGOCIO_ID, INSTANTE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan gratuito no encontrado");

        verifyNoInteractions(suscripcionRepository);
    }

    @Test
    @DisplayName("Obtiene la suscripción solamente por el negocio recibido del servidor")
    void obtenerShouldReturnSubscriptionOfNegocio() {
        Suscripcion esperada = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(esperada));

        Suscripcion result = suscripcionService.obtener(NEGOCIO_ID);

        assertThat(result).isSameAs(esperada);
        verify(suscripcionRepository).findByNegocioId(NEGOCIO_ID);
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void obtenerShouldRejectMissingSubscription() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionService.obtener(NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Suscripción no encontrada");
    }

    // --- arrange ---
    private void givenFreePlanAvailable() {
        when(planRepository.findByCodigo(CodigoPlan.GRATUITO)).thenReturn(Optional.of(plan()));
        when(suscripcionRepository.save(any())).thenReturn(suscripcion());
    }

    // --- assert ---
    private void thenFreeSubscriptionSaved(Suscripcion result) {
        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).save(captor.capture());
        Suscripcion guardada = captor.getValue();
        assertThat(guardada.getId()).isNull();
        assertThat(guardada.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(guardada.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(guardada.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(guardada.getFinEn()).isNull();
        assertThat(guardada.getCreadoEn()).isEqualTo(INSTANTE);
        assertThat(guardada.getActualizadoEn()).isEqualTo(INSTANTE);
        assertThat(result.getId()).isEqualTo(suscripcion().getId());
    }
}
