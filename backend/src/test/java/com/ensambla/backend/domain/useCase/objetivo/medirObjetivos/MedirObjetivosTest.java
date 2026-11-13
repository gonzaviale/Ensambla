package com.ensambla.backend.domain.useCase.objetivo.medirObjetivos;

import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.objetivo.MonitoreoObjetivosService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedirObjetivosTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MonitoreoObjetivosService monitoreoObjetivosService = mock(MonitoreoObjetivosService.class);
    private final MedirObjetivos medirObjetivos =
            new MedirObjetivos(negocioRepository, monitoreoObjetivosService, new TransaccionDirecta(), CLOCK);

    @Test
    @DisplayName("Mide en el momento los objetivos del negocio de la sesión")
    void executeShouldMonitorSessionNegocioNow() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);

        medirObjetivos.execute(new MedirObjetivosRequest(USUARIO_ID));

        verify(monitoreoObjetivosService).monitorear(NEGOCIO_ID, OffsetDateTime.now(CLOCK));
    }
}
