package com.ensambla.backend.domain.useCase.plan.vencerPruebas;

import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class VencerPruebasTest {

    private final PruebaPremiumService pruebaPremiumService = mock(PruebaPremiumService.class);
    private final VencerPruebas vencerPruebas = new VencerPruebas(
            pruebaPremiumService, Clock.fixed(INSTANTE.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Vence cada candidato con un único instante obtenido del reloj")
    void executeShouldExpireCandidatesUsingServerClock() {
        when(pruebaPremiumService.listarVencidas(INSTANTE)).thenReturn(List.of(NEGOCIO_ID));

        vencerPruebas.execute();

        verify(pruebaPremiumService).listarVencidas(INSTANTE);
        verify(pruebaPremiumService).vencer(NEGOCIO_ID, INSTANTE);
    }

    @Test
    @DisplayName("Un lote sin pruebas vencidas no modifica suscripciones")
    void executeShouldSkipEmptyBatch() {
        when(pruebaPremiumService.listarVencidas(INSTANTE)).thenReturn(List.of());

        vencerPruebas.execute();

        verify(pruebaPremiumService).listarVencidas(INSTANTE);
        verifyNoMoreInteractions(pruebaPremiumService);
    }
}
