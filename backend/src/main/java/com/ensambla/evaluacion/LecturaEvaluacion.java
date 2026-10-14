package com.ensambla.evaluacion;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.infrastructure.ia.LectorCuadernoTimeoutAdapter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class LecturaEvaluacion {

    private final LectorCuadernoPort lectorCuadernoPort;
    private final ObjectMapper objectMapper;

    public void ejecutar(Path entrada, Path salida, Duration timeout) throws IOException {
        List<ResultadoEvaluacion> resultados = new ArrayList<>();
        SolicitudEvaluacion solicitud = objectMapper.readValue(Files.readString(entrada), SolicitudEvaluacion.class);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        try {
            var lector = new LectorCuadernoTimeoutAdapter(lectorCuadernoPort, executorService, timeout);
            for (CasoEvaluacion caso : solicitud.casos()) {
                resultados.add(leer(caso, lector));
            }
        } finally {
            executorService.shutdownNow();
        }
        Files.writeString(salida, objectMapper.writeValueAsString(resultados));
    }

    private ResultadoEvaluacion leer(CasoEvaluacion caso, LectorCuadernoPort lector) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(caso.archivo()));
        try {
            LecturaCuaderno lectura = lector.leer(
                    List.of(new PaginaCuaderno(1, bytes, caso.tipoContenido())), List.of(), null);
            return new ResultadoEvaluacion(caso.id(), lectura, null);
        } catch (LecturaCuadernoException e) {
            return new ResultadoEvaluacion(caso.id(), null, e.getMessage());
        }
    }

    public record SolicitudEvaluacion(List<CasoEvaluacion> casos) {
    }

    public record CasoEvaluacion(String id, String archivo, String tipoContenido) {
    }

    public record ResultadoEvaluacion(String id, LecturaCuaderno lectura, String error) {
    }
}
