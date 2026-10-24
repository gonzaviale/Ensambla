package com.ensambla.backend.domain.useCase.procesamiento.procesarLote;

import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcesarLoteTest {

    @Test
    @DisplayName("Recupera reclamos y procesa un lote con el reloj compartido")
    void executeShouldProcessClaimedWorkWithClock() {
        ProcesamientoService procesamientoService = mock(ProcesamientoService.class);
        LoteTrabajo lote = ProcesamientoTestData.lote(1);
        ResultadoIntento resultado = new ResultadoIntento(ProcesamientoTestData.propuesta(), null, false);
        when(procesamientoService.reclamar(AHORA)).thenReturn(Optional.of(lote));
        when(procesamientoService.leer(lote, AHORA)).thenReturn(resultado);
        var useCase = new ProcesarLote(procesamientoService, Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

        useCase.execute();

        verify(procesamientoService).recuperar(AHORA);
        verify(procesamientoService).finalizar(lote, resultado, AHORA);
    }

    @Test
    @DisplayName("No invoca al lector cuando no hay trabajo pendiente")
    void executeShouldStopWhenQueueEmpty() {
        ProcesamientoService procesamientoService = mock(ProcesamientoService.class);
        when(procesamientoService.reclamar(AHORA)).thenReturn(Optional.empty());
        var useCase = new ProcesarLote(procesamientoService, Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

        useCase.execute();

        verify(procesamientoService).recuperar(AHORA);
    }
}
