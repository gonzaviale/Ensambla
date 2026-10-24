package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.service.procesamiento.ValidacionLecturaService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del puerto de lectura: arma la petición, llama al proveedor a través de {@link ClienteModeloVision},
 * valida la salida estructurada y devuelve el resultado junto con el consumo de la llamada.
 */
@Slf4j
public class LectorCuadernoIaAdapter implements LectorCuadernoPort {

    static final String ERROR_PROVEEDOR =
            "No pudimos leer el cuaderno en este momento. Probá de nuevo en unos minutos.";
    static final String ERROR_JSON = LecturaIaJsonMapper.PREFIJO_ERROR + "la respuesta no es el JSON esperado";

    private final ClienteModeloVision cliente;
    private final PromptLecturaCuaderno prompt;
    private final LecturaIaJsonMapper mapper;
    private final ObjectMapper objectMapper;
    private final int maxPaginas;

    public LectorCuadernoIaAdapter(ClienteModeloVision cliente, PromptLecturaCuaderno prompt, ObjectMapper objectMapper,
                                   int maxPaginas) {
        this.cliente = cliente;
        this.prompt = prompt;
        this.mapper = new LecturaIaJsonMapper();
        this.objectMapper = objectMapper;
        this.maxPaginas = maxPaginas;
    }

    @Override
    public LecturaCuaderno leer(List<PaginaCuaderno> paginas, List<BloqueConocido> bloquesExistentes,
                                UUID bloqueDestinoId) {
        validarCantidad(paginas);
        PeticionModeloVision peticion = prompt.peticion(paginas, bloquesExistentes, bloqueDestinoId);
        long inicio = System.nanoTime();
        RespuestaModeloVision respuesta = invocar(peticion);
        long duracionMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        ConsumoIa consumo = new ConsumoIa(respuesta.proveedor(), respuesta.modelo(), PromptLecturaCuaderno.VERSION,
                paginas.size(), respuesta.tokensEntrada(), respuesta.tokensSalida(), duracionMs);
        LecturaCuaderno lectura = mapper.toDomain(
                parsear(respuesta.json()), bloquesExistentes, paginas.size(), consumo);
        new ValidacionLecturaService().validar(lectura);
        return lectura;
    }

    private void validarCantidad(List<PaginaCuaderno> paginas) {
        if (paginas.isEmpty() || paginas.size() > maxPaginas) {
            throw new LecturaCuadernoException(
                    "Se pueden leer entre 1 y " + maxPaginas + " páginas por vez", null, false);
        }
    }

    private RespuestaModeloVision invocar(PeticionModeloVision peticion) {
        try {
            return cliente.completar(peticion);
        } catch (LecturaCuadernoException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Falló la llamada al proveedor de IA: {}", e.getMessage());
            throw new LecturaCuadernoException(ERROR_PROVEEDOR, e);
        }
    }

    private LecturaIaJson parsear(String json) {
        if (json == null || json.isBlank()) {
            throw new LecturaCuadernoException(ERROR_JSON, null, false);
        }
        try {
            new ValidacionEstructuraIa().validar(objectMapper.reader()
                    .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION).readTree(json));
            return objectMapper.readerFor(LecturaIaJson.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                            DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(json);
        } catch (JacksonException e) {
            throw new LecturaCuadernoException(ERROR_JSON, e, false);
        }
    }
}
