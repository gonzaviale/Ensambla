package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LectorCuadernoTimeoutAdapterTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private Future<LecturaCuaderno> futuro;

    @Mock
    private LectorCuadernoPort lectorCuadernoPort;

    private LectorCuadernoTimeoutAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorService, Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Devuelve el resultado del lector dentro del plazo")
    void leerShouldReturnDelegateResult() {
        try (ExecutorService real = Executors.newSingleThreadExecutor()) {
            when(lectorCuadernoPort.leer(List.of(), List.of())).thenReturn(ProcesamientoTestData.lectura());
            var lector = new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, real, Duration.ofSeconds(5));

            LecturaCuaderno resultado = lector.leer(List.of(), List.of());

            assertThat(resultado).isEqualTo(ProcesamientoTestData.lectura());
        }
    }

    @Test
    @DisplayName("Cancela la tarea cuando se agota el timeout")
    void leerShouldCancelTimedOutTask() throws Exception {
        givenFuture();
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException());

        assertThatThrownBy(() -> adapter.leer(List.of(), List.of())).hasMessageContaining("tiempo de respuesta");
        verify(futuro).cancel(true);
    }

    @Test
    @DisplayName("Cancela y conserva la interrupción del worker")
    void leerShouldRestoreInterruptedFlag() throws Exception {
        givenFuture();
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());

        try {
            assertThatThrownBy(() -> adapter.leer(List.of(), List.of())).hasMessageContaining("interrumpió");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(futuro).cancel(true);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("Conserva un error de lectura del proveedor")
    void leerShouldPreserveDomainFailure() throws Exception {
        givenFuture();
        LecturaCuadernoException error = new LecturaCuadernoException("Salida inválida", null, false);
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new ExecutionException(error));

        assertThatThrownBy(() -> adapter.leer(List.of(), List.of())).isSameAs(error);
    }

    @Test
    @DisplayName("Oculta el mensaje técnico de errores del proveedor")
    void leerShouldWrapUnexpectedFailure() throws Exception {
        givenFuture();
        when(futuro.get(100, TimeUnit.MILLISECONDS))
                .thenThrow(new ExecutionException(new IllegalStateException("privado")));

        assertThatThrownBy(() -> adapter.leer(List.of(), List.of()))
                .hasMessage(LectorCuadernoIaAdapter.ERROR_PROVEEDOR);
    }

    private void givenFuture() {
        when(executorService.submit(this.<Callable<LecturaCuaderno>>givenAnyTask())).thenReturn(futuro);
    }

    private <T> T givenAnyTask() {
        return any();
    }
}
