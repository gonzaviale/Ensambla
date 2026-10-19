package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.service.aviso.MotorAvisosService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EjecutarMotorAvisosUseCaseTest {

    private static final UUID NEGOCIO_UNO = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_DOS = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final Instant INSTANTE = Instant.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final MotorAvisosService motorAvisosService = mock(MotorAvisosService.class);
    private final EjecutarMotorAvisosUseCase useCase = new EjecutarMotorAvisosUseCase(
            negocioService, motorAvisosService, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("Corre el motor sobre cada negocio con el instante del reloj")
    void executeShouldRunEngineForEveryNegocio() {
        when(negocioService.listarIds()).thenReturn(List.of(NEGOCIO_UNO, NEGOCIO_DOS));

        useCase.execute();

        verify(motorAvisosService).ejecutar(NEGOCIO_UNO, AHORA);
        verify(motorAvisosService).ejecutar(NEGOCIO_DOS, AHORA);
    }

    @Test
    @DisplayName("Un negocio con error no frena a los demás")
    void executeShouldContinueWhenOneNegocioFails() {
        when(negocioService.listarIds()).thenReturn(List.of(NEGOCIO_UNO, NEGOCIO_DOS));
        doThrow(new IllegalStateException("Base no disponible")).when(motorAvisosService)
                .ejecutar(NEGOCIO_UNO, AHORA);

        assertThatCode(useCase::execute).doesNotThrowAnyException();

        verify(motorAvisosService).ejecutar(NEGOCIO_DOS, AHORA);
    }
}
