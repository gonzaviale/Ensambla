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
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private LecturaCuaderno lectura;
    private Throwable rechazo;
    private LecturaCuadernoException errorDeDominio;

    @BeforeEach
    void setUp() {
        adapter = new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorService, Duration.ofMillis(100));
    }

    @Test
    @DisplayName("Devuelve el resultado del lector dentro del plazo")
    void devuelveElResultadoDelLectorDentroDelPlazo() {
        dadoUnLectorQueDevuelveUnaLecturaDentroDelPlazo();

        cuandoSeLeeEnUnEjecutorReal();

        entoncesSeDevuelveElResultadoDelLector();
    }

    @Test
    @DisplayName("Cancela la tarea cuando se agota el timeout")
    void cancelaLaTareaCuandoSeAgotaElTimeout() throws Exception {
        dadoUnaTareaQueSeAgotaPorTimeout();

        cuandoSeIntentaLeer();

        entoncesSeAvisaElTimeoutYSeCancelaLaTarea();
    }

    @Test
    @DisplayName("Cancela y conserva la interrupción del worker")
    void cancelaYConservaLaInterrupcionDelWorker() throws Exception {
        dadoUnaTareaInterrumpida();

        cuandoSeIntentaLeer();

        entoncesSeAvisaLaInterrupcionSeCancelaYSeConservaLaMarca();
    }

    @Test
    @DisplayName("Conserva un error de lectura del proveedor")
    void conservaUnErrorDeLecturaDelProveedor() throws Exception {
        dadoUnaTareaQueFallaConUnErrorDeLecturaDeDominio();

        cuandoSeIntentaLeer();

        entoncesSePropagaElErrorDeDominio();
    }

    @Test
    @DisplayName("Oculta el mensaje técnico de errores del proveedor")
    void ocultaElMensajeTecnicoDeErroresDelProveedor() throws Exception {
        dadoUnaTareaQueFallaConUnErrorTecnico();

        cuandoSeIntentaLeer();

        entoncesSeOcultaElMensajeTecnico();
    }

    // --- dado ---

    private void dadoUnLectorQueDevuelveUnaLecturaDentroDelPlazo() {
        when(lectorCuadernoPort.leer(List.of(), List.of(), null)).thenReturn(ProcesamientoTestData.lectura());
    }

    private void dadoUnaTareaQueSeAgotaPorTimeout() throws Exception {
        dadoUnFuturoEnElEjecutor();
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException());
    }

    private void dadoUnaTareaInterrumpida() throws Exception {
        dadoUnFuturoEnElEjecutor();
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());
    }

    private void dadoUnaTareaQueFallaConUnErrorDeLecturaDeDominio() throws Exception {
        dadoUnFuturoEnElEjecutor();
        errorDeDominio = new LecturaCuadernoException("Salida inválida", null, false);
        when(futuro.get(100, TimeUnit.MILLISECONDS)).thenThrow(new ExecutionException(errorDeDominio));
    }

    private void dadoUnaTareaQueFallaConUnErrorTecnico() throws Exception {
        dadoUnFuturoEnElEjecutor();
        when(futuro.get(100, TimeUnit.MILLISECONDS))
                .thenThrow(new ExecutionException(new IllegalStateException("privado")));
    }

    private void dadoUnFuturoEnElEjecutor() {
        when(executorService.submit(this.<Callable<LecturaCuaderno>>cualquierTarea())).thenReturn(futuro);
    }

    // --- cuando ---

    private void cuandoSeLeeEnUnEjecutorReal() {
        try (ExecutorService real = Executors.newSingleThreadExecutor()) {
            var lector = new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, real, Duration.ofSeconds(5));
            lectura = lector.leer(List.of(), List.of(), null);
        }
    }

    private void cuandoSeIntentaLeer() {
        rechazo = catchThrowable(() -> adapter.leer(List.of(), List.of(), null));
    }

    // --- entonces ---

    private void entoncesSeDevuelveElResultadoDelLector() {
        assertThat(lectura).isEqualTo(ProcesamientoTestData.lectura());
    }

    private void entoncesSeAvisaElTimeoutYSeCancelaLaTarea() {
        assertThat(rechazo).hasMessageContaining("tiempo de respuesta");
        verify(futuro).cancel(true);
    }

    private void entoncesSeAvisaLaInterrupcionSeCancelaYSeConservaLaMarca() {
        try {
            assertThat(rechazo).hasMessageContaining("interrumpió");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(futuro).cancel(true);
        } finally {
            Thread.interrupted();
        }
    }

    private void entoncesSePropagaElErrorDeDominio() {
        assertThat(rechazo).isSameAs(errorDeDominio);
    }

    private void entoncesSeOcultaElMensajeTecnico() {
        assertThat(rechazo).hasMessage(LectorCuadernoIaAdapter.ERROR_PROVEEDOR);
    }

    // --- datos ---

    private <T> T cualquierTarea() {
        return any();
    }
}
