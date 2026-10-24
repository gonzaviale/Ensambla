package com.ensambla.evaluacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import com.ensambla.backend.infrastructure.ia.LectorCuadernoTimeoutAdapter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class LecturaEvaluacion {

    private final LectorCuadernoPort lectorCuadernoPort;
    private final ObjectMapper objectMapper;
    private final PropuestaLecturaService propuestaLecturaService = new PropuestaLecturaService();

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
        List<PaginaCuaderno> paginas = List.of(new PaginaCuaderno(1, bytes, caso.tipoContenido()));
        try {
            if (caso.destino() == null) {
                return new ResultadoEvaluacion(caso.id(), lector.leer(paginas, List.of(), null), null);
            }
            return leerEnDestino(caso, paginas, lector);
        } catch (LecturaCuadernoException | BusinessRuleException e) {
            return new ResultadoEvaluacion(caso.id(), null, e.getMessage());
        }
    }

    /**
     * Igual que al subir las fotos a un bloque elegido: la IA ve sólo ese bloque y después se separan las columnas
     * que coinciden de las que el bloque no tiene (las nuevas), con las mismas reglas que la lectura real.
     */
    private ResultadoEvaluacion leerEnDestino(CasoEvaluacion caso, List<PaginaCuaderno> paginas,
                                              LectorCuadernoPort lector) {
        DestinoEvaluacion destino = caso.destino();
        UUID destinoId = UUID.randomUUID();
        List<CampoContexto> contexto = new ArrayList<>();
        List<CampoConocido> conocidos = new ArrayList<>();
        for (CampoDestino campo : destino.campos()) {
            TipoCampo tipo = TipoCampo.valueOf(campo.tipo());
            List<String> opciones = campo.opciones() == null ? List.of() : campo.opciones();
            contexto.add(new CampoContexto(campo.nombre(), tipo, opciones));
            conocidos.add(new CampoConocido(UUID.randomUUID(), campo.nombre(), tipo, opciones));
        }
        LecturaCuaderno lectura = lector.leer(
                paginas, List.of(new BloqueConocido(destinoId, destino.nombre(), contexto)), destinoId);
        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                lectura, List.of(new BloqueLectura(destinoId, destino.nombre(), conocidos)), destinoId);
        return new ResultadoEvaluacion(caso.id(), propuesta.lectura(), null, propuesta.camposNuevos());
    }

    public record SolicitudEvaluacion(List<CasoEvaluacion> casos) {
    }

    /** @param destino bloque existente al que van las filas; {@code null} si la foto se lee sin bloque elegido */
    public record CasoEvaluacion(String id, String archivo, String tipoContenido, DestinoEvaluacion destino) {

        public CasoEvaluacion(String id, String archivo, String tipoContenido) {
            this(id, archivo, tipoContenido, null);
        }
    }

    public record DestinoEvaluacion(String nombre, List<CampoDestino> campos) {
    }

    public record CampoDestino(String nombre, String tipo, List<String> opciones) {
    }

    /** @param camposNuevos columnas de la foto que el bloque destino no tiene; vacía sin destino */
    public record ResultadoEvaluacion(String id, LecturaCuaderno lectura, String error,
                                      List<CampoPropuesto> camposNuevos) {

        public ResultadoEvaluacion {
            camposNuevos = camposNuevos == null ? List.of() : List.copyOf(camposNuevos);
        }

        public ResultadoEvaluacion(String id, LecturaCuaderno lectura, String error) {
            this(id, lectura, error, List.of());
        }
    }
}
