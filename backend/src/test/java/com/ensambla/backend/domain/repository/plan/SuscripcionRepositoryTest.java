package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PlanTestData.suscripcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuscripcionRepositoryTest {

    private final SuscripcionRepository suscripcionRepository = mock(SuscripcionRepository.class, CALLS_REAL_METHODS);

    private Suscripcion esperada;
    private Suscripcion suscripcionObtenida;
    private Throwable excepcion;

    @Test
    @DisplayName("Obtiene la suscripción solamente por el negocio recibido del servidor")
    void obtieneLaSuscripcionSolamentePorElNegocioRecibidoDelServidor() {
        dadoUnNegocioConSuscripcion();

        cuandoSeObtieneLaSuscripcionDelNegocio();

        entoncesSeDevuelveLaSuscripcionDelNegocio();
    }

    @Test
    @DisplayName("Responde recurso inexistente cuando el negocio no tiene suscripción")
    void respondeRecursoInexistenteCuandoElNegocioNoTieneSuscripcion() {
        dadoUnNegocioSinSuscripcion();

        cuandoSeIntentaObtenerLaSuscripcionDelNegocio();

        entoncesSeRechazaPorqueLaSuscripcionNoSeEncontro();
    }

    // --- dado ---
    private void dadoUnNegocioConSuscripcion() {
        esperada = suscripcion();
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(esperada));
    }

    private void dadoUnNegocioSinSuscripcion() {
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeObtieneLaSuscripcionDelNegocio() {
        suscripcionObtenida = suscripcionRepository.obtener(NEGOCIO_ID);
    }

    private void cuandoSeIntentaObtenerLaSuscripcionDelNegocio() {
        excepcion = catchThrowable(() -> suscripcionRepository.obtener(NEGOCIO_ID));
    }

    // --- entonces ---
    private void entoncesSeDevuelveLaSuscripcionDelNegocio() {
        assertThat(suscripcionObtenida).isSameAs(esperada);
    }

    private void entoncesSeRechazaPorqueLaSuscripcionNoSeEncontro() {
        assertThat(excepcion).isInstanceOf(ResourceNotFoundException.class).hasMessage("Suscripción no encontrada");
    }
}
