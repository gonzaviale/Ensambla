package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.EleccionIa;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Conversa un objetivo con un modelo de texto. Sólo lee los campos del esquema ({@code ia/esquema-conversacion-objetivos
 * .json}) y descarta el resto; lo que pide la persona no cambia lo que se acepta. Un mini-objetivo con un modo que no
 * existe se ignora. Si el JSON no se puede leer lanza {@link IllegalStateException} y quien llama arma el respaldo;
 * si el modelo no respondió falla con la excepción del cliente. La validación de fondo (catálogo, cifras, cantidad) la
 * hace el dominio.
 */
public class ConversacionObjetivosIaAdapter implements ConversacionObjetivosIaPort {

    /** Forma del JSON que devuelve el modelo, antes de validarlo. */
    record RespuestaJson(String respuesta, String entendido, Boolean esMonetario, List<MiniJson> minis) {

        record MiniJson(String modo, String codigo, String titulo, String justificacion) {
        }
    }

    private final ClienteModeloTexto modelo;
    private final PromptConversacionObjetivos prompt;
    private final ObjectMapper objectMapper;

    public ConversacionObjetivosIaAdapter(
            ClienteModeloTexto modelo, PromptConversacionObjetivos prompt, ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public PropuestaObjetivoIa conversar(ConsultaObjetivoIa consulta) {
        RespuestaModeloTexto respuesta = modelo.completar(prompt.peticion(consulta));
        ConsumoIaSami consumo = new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(),
                respuesta.tokensEntrada(), respuesta.tokensSalida());
        RespuestaJson leida = leer(respuesta.json());
        List<RespuestaJson.MiniJson> minis = leida.minis() == null ? List.of() : leida.minis();
        return new PropuestaObjetivoIa(
                leida.respuesta(),
                leida.entendido(),
                Boolean.TRUE.equals(leida.esMonetario()),
                minis.stream().filter(Objects::nonNull).map(ConversacionObjetivosIaAdapter::eleccion)
                        .filter(Objects::nonNull).toList(),
                consumo);
    }

    private RespuestaJson leer(String json) {
        try {
            return objectMapper.readerFor(RespuestaJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (RuntimeException e) {
            // JSON mal formado, vacío o con campos de otro tipo
            throw new IllegalStateException("La IA devolvió una respuesta ilegible", e);
        }
    }

    /** La elección de la IA; {@code null} si el modo no es uno de los dos. */
    private static EleccionIa eleccion(RespuestaJson.MiniJson mini) {
        if (mini.modo() == null) {
            return null;
        }
        return switch (mini.modo().strip().toUpperCase(Locale.ROOT)) {
            case "MEDIDO" -> new EleccionIa(ModoMiniObjetivo.MEDIDO, mini.codigo(), null, null);
            case "MANUAL" -> new EleccionIa(ModoMiniObjetivo.MANUAL, null, mini.titulo(), mini.justificacion());
            default -> null;
        };
    }
}
