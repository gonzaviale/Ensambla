package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.TextoMiniObjetivo;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Redacta los mini-objetivos con un modelo de texto. Sólo acepta una respuesta que traiga un texto válido para cada
 * candidato (ni más ni menos, con título y justificación que no pasen del largo permitido); con cualquier otra cosa
 * lanza {@link IllegalStateException} y quien llama usa el texto de reserva. Si el modelo no respondió falla con la
 * excepción del cliente.
 */
public class RedaccionObjetivosIaAdapter implements RedaccionObjetivosIaPort {

    static final int LARGO_MAXIMO_TITULO = 80;
    static final int LARGO_MAXIMO_JUSTIFICACION = 280;

    /** Forma del JSON que devuelve el modelo ({@code ia/esquema-objetivos.json}), antes de validarlo. */
    record RespuestaJson(List<MiniJson> minis) {

        record MiniJson(String codigo, String titulo, String justificacion) {
        }
    }

    private final ClienteModeloTexto modelo;
    private final PromptObjetivos prompt;
    private final ObjectMapper objectMapper;

    public RedaccionObjetivosIaAdapter(ClienteModeloTexto modelo, PromptObjetivos prompt, ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public RedaccionObjetivosIa redactar(List<CandidatoMiniObjetivo> candidatos) {
        RespuestaModeloTexto respuesta = modelo.completar(prompt.peticion(candidatos));
        ConsumoIaSami consumo = new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(),
                respuesta.tokensEntrada(), respuesta.tokensSalida());
        Map<String, RespuestaJson.MiniJson> porCodigo = leer(respuesta.json());
        List<TextoMiniObjetivo> textos = IntStream.range(0, candidatos.size())
                .mapToObj(indice -> texto(porCodigo.get(PromptObjetivos.codigo(indice))))
                .toList();
        return new RedaccionObjetivosIa(textos, consumo);
    }

    private Map<String, RespuestaJson.MiniJson> leer(String json) {
        try {
            RespuestaJson leida = objectMapper.readerFor(RespuestaJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
            return leida.minis().stream()
                    .collect(Collectors.toMap(RespuestaJson.MiniJson::codigo, Function.identity()));
        } catch (RuntimeException e) {
            // JSON mal formado, sin lista de textos, con un código repetido o sin código
            throw new IllegalStateException("La IA devolvió una redacción ilegible", e);
        }
    }

    private static TextoMiniObjetivo texto(RespuestaJson.MiniJson mini) {
        if (mini == null || vacioOLargo(mini.titulo(), LARGO_MAXIMO_TITULO)
                || vacioOLargo(mini.justificacion(), LARGO_MAXIMO_JUSTIFICACION)) {
            throw new IllegalStateException("La IA no redactó un texto válido para cada mini-objetivo");
        }
        return new TextoMiniObjetivo(mini.titulo().strip(), mini.justificacion().strip());
    }

    private static boolean vacioOLargo(String texto, int maximo) {
        return texto == null || texto.isBlank() || texto.strip().length() > maximo;
    }
}
