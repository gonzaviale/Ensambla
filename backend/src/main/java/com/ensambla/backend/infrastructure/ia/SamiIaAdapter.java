package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.port.SamiIaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Respaldo con IA de Sami (T-117). Prueba los modelos en orden, del más chico al más capaz: se queda con el primero
 * cuya propuesta es válida y llega a {@code umbralEscalar}; si no, escala al siguiente. Así el modelo caro sólo se
 * paga en los casos difíciles. Suma los tokens de todos los que se consultaron.
 * <p>
 * Una respuesta ilegible o con una plantilla inexistente no lanza error: vuelve como propuesta sin plantilla y con
 * su consumo, para que se registre lo gastado. Sólo falla (con la excepción del cliente) si el primer modelo no
 * respondió; si falla uno posterior, se conserva el resultado del anterior.
 */
public class SamiIaAdapter implements SamiIaPort {

    private static final Logger log = LoggerFactory.getLogger(SamiIaAdapter.class);

    private final List<ClienteModeloTexto> modelos;
    private final PromptSami prompt;
    private final ObjectMapper objectMapper;
    private final double umbralEscalar;

    public SamiIaAdapter(List<ClienteModeloTexto> modelos, PromptSami prompt, ObjectMapper objectMapper,
                         double umbralEscalar) {
        this.modelos = List.copyOf(modelos);
        this.prompt = prompt;
        this.objectMapper = objectMapper;
        this.umbralEscalar = umbralEscalar;
    }

    @Override
    public PropuestaIaSami proponer(ConsultaIaSami consulta) {
        PeticionModeloTexto peticion = prompt.peticion(consulta);
        ConsumoIaSami consumo = null;
        PropuestaIaSami mejor = null;
        for (ClienteModeloTexto modelo : modelos) {
            Optional<RespuestaModeloTexto> respuesta = consultar(modelo, peticion, consumo == null);
            if (respuesta.isEmpty()) {
                break;
            }
            ConsumoIaSami gastado = new ConsumoIaSami(respuesta.get().proveedor(), respuesta.get().modelo(),
                    respuesta.get().tokensEntrada(), respuesta.get().tokensSalida());
            consumo = consumo == null ? gastado : consumo.sumar(gastado);
            Optional<PropuestaIaSami> propuesta = leer(respuesta.get().json(), consumo);
            if (propuesta.isPresent() && propuesta.get().aInterpretacion(consulta.plantillas()).isPresent()) {
                mejor = propuesta.get();
                if (mejor.confianza() >= umbralEscalar) {
                    break;
                }
            }
        }
        return mejor != null ? conConsumo(mejor, consumo) : new PropuestaIaSami(null, List.of(), 0.0, consumo);
    }

    private Optional<RespuestaModeloTexto> consultar(ClienteModeloTexto modelo, PeticionModeloTexto peticion,
                                                     boolean esElPrimero) {
        try {
            return Optional.of(modelo.completar(peticion));
        } catch (RuntimeException e) {
            if (esElPrimero) {
                throw e;
            }
            log.warn("Falló un modelo de respaldo de Sami; se usa el resultado anterior: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private PropuestaIaSami conConsumo(PropuestaIaSami propuesta, ConsumoIaSami consumo) {
        return new PropuestaIaSami(propuesta.plantilla(), propuesta.columnasExtra(), propuesta.confianza(), consumo);
    }

    private Optional<PropuestaIaSami> leer(String json, ConsumoIaSami consumo) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            PropuestaSamiJson leida = objectMapper.readerFor(PropuestaSamiJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
            return Optional.ofNullable(leida).map(propuesta -> new PropuestaIaSami(
                    propuesta.plantilla(),
                    columnas(propuesta),
                    propuesta.confianza() == null ? 0.0 : propuesta.confianza(),
                    consumo));
        } catch (JacksonException e) {
            log.warn("Sami recibió un JSON ilegible de la IA: {}", e.getOriginalMessage());
            return Optional.empty();
        }
    }

    private List<ColumnaSugerida> columnas(PropuestaSamiJson leida) {
        List<ColumnaSugerida> columnas = new ArrayList<>();
        if (leida.columnas() == null) {
            return columnas;
        }
        for (PropuestaSamiJson.ColumnaJson columna : leida.columnas()) {
            tipoDe(columna.tipo()).ifPresent(tipo -> columnas.add(
                    new ColumnaSugerida(columna.nombre(), tipo, columna.opciones())));
        }
        return columnas;
    }

    private Optional<TipoCampo> tipoDe(String tipo) {
        try {
            return Optional.of(TipoCampo.valueOf(tipo));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
