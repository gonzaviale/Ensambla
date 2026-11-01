package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuscripcionRepositoryTest {

    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene la suscripción solamente por el negocio recibido del servidor")
    void obtenerShouldReturnSubscriptionOfNegocio() {
        Suscripcion esperada = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(esperada));

        Suscripcion result = suscripcionRepository.obtener(NEGOCIO_ID);

        assertThat(result).isSameAs(esperada);
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void obtenerShouldRejectMissingSubscription() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suscripcionRepository.obtener(NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Suscripción no encontrada");
    }
}
