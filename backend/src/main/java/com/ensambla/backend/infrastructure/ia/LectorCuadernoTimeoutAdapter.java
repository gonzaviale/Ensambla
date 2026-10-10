package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
public class LectorCuadernoTimeoutAdapter implements LectorCuadernoPort {

    private final LectorCuadernoPort lectorCuadernoPort;
    private final ExecutorService executorService;
    private final Duration timeout;

    @Override
    public LecturaCuaderno leer(List<PaginaCuaderno> paginas, List<BloqueConocido> bloques) {
        Future<LecturaCuaderno> futuro = executorService.submit(() -> lectorCuadernoPort.leer(paginas, bloques));
        try {
            return futuro.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new LecturaCuadernoException("Se agotó el tiempo de respuesta de la IA", e);
        } catch (InterruptedException e) {
            futuro.cancel(true);
            Thread.currentThread().interrupt();
            throw new LecturaCuadernoException("Se interrumpió la lectura del cuaderno", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof LecturaCuadernoException lecturaException) {
                throw lecturaException;
            }
            throw new LecturaCuadernoException(LectorCuadernoIaAdapter.ERROR_PROVEEDOR, e.getCause());
        }
    }
}
