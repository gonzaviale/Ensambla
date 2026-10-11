package com.ensambla.evaluacion;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LecturaEvaluacionTest {

    @TempDir
    private Path carpeta;

    @Test
    @DisplayName("El comando real usa el mismo lector simulado sin levantar servidor ni base")
    void mainShouldRunExtractionWithoutDatabase() throws Exception {
        Path entrada = givenSolicitud();
        Path salida = carpeta.resolve("salida.json");

        EvaluacionIaApplication.main(new String[]{
                "--evaluacion.entrada=" + entrada, "--evaluacion.salida=" + salida,
                "--ensambla.ia.proveedor=simulado", "--spring.config.import=",
                "--spring.main.banner-mode=off", "--logging.level.root=WARN"});

        var resultado = JsonMapper.builder().build().readTree(Files.readString(salida));
        assertThat(resultado.get(0).path("id").asString()).isEqualTo("sintetico-001");
        assertThat(resultado.get(0).path("lectura").path("tipoRegistro").asString()).isEqualTo("VENTAS");
        assertThat(resultado.get(0).path("lectura").path("consumo").path("proveedor").asString()).isEqualTo("simulado");
    }

    @Test
    @DisplayName("Registra fallos de extracción por caso sin producir lecturas ficticias")
    void ejecutarShouldRecordExtractionError() throws Exception {
        Path entrada = givenSolicitud();
        Path salida = carpeta.resolve("salida.json");
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any())).thenThrow(new LecturaCuadernoException("Falla"));
        var evaluacion = new LecturaEvaluacion(lector, JsonMapper.builder().build());

        evaluacion.ejecutar(entrada, salida, Duration.ofSeconds(5));

        var resultado = JsonMapper.builder().build().readTree(Files.readString(salida));
        assertThat(resultado.get(0).path("error").asString()).isEqualTo("Falla");
        assertThat(resultado.get(0).path("lectura").isNull()).isTrue();
    }

    private Path givenSolicitud() throws Exception {
        Path foto = carpeta.resolve("sintetico.jpg");
        Files.write(foto, new byte[]{1, 2, 3});
        Path entrada = carpeta.resolve("entrada.json");
        var solicitud = new LecturaEvaluacion.SolicitudEvaluacion(
                List.of(new LecturaEvaluacion.CasoEvaluacion("sintetico-001", foto.toString(), "image/jpeg")));
        Files.writeString(entrada, JsonMapper.builder().build().writeValueAsString(solicitud));
        return entrada;
    }
}
