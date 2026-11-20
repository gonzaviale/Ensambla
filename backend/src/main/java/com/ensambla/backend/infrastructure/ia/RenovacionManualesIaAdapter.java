package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.TextoMiniObjetivo;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RenovacionManualesIaPort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Redacta las acciones que renuevan los mini-objetivos manuales con un modelo de texto. Igual que la redacción de los
 * mini-objetivos, sólo acepta una respuesta con un texto válido para cada pedido (ni más ni menos, con el largo
 * permitido); con cualquier otra cosa lanza {@link IllegalStateException} y quien llama usa el texto de reserva. El
 * dominio vuelve a validar cada texto como acción.
 */
public class RenovacionManualesIaAdapter implements RenovacionManualesIaPort {

    private final ClienteModeloTexto modelo;
    private final PromptRenovacionManuales prompt;
    private final ObjectMapper objectMapper;

    public RenovacionManualesIaAdapter(
            ClienteModeloTexto modelo, PromptRenovacionManuales prompt, ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public RedaccionObjetivosIa renovar(List<PedidoRenovacion> pedidos) {
        RespuestaModeloTexto respuesta = modelo.completar(prompt.peticion(pedidos));
        ConsumoIaSami consumo = new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(),
                respuesta.tokensEntrada(), respuesta.tokensSalida());
        Map<String, RedaccionObjetivosIaAdapter.RespuestaJson.MiniJson> porCodigo = leer(respuesta.json());
        List<TextoMiniObjetivo> textos = IntStream.range(0, pedidos.size())
                .mapToObj(indice -> texto(porCodigo.get(PromptRenovacionManuales.codigo(indice))))
                .toList();
        return new RedaccionObjetivosIa(textos, consumo);
    }

    private Map<String, RedaccionObjetivosIaAdapter.RespuestaJson.MiniJson> leer(String json) {
        try {
            RedaccionObjetivosIaAdapter.RespuestaJson leida = objectMapper
                    .readerFor(RedaccionObjetivosIaAdapter.RespuestaJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
            return leida.minis().stream()
                    .collect(Collectors.toMap(
                            RedaccionObjetivosIaAdapter.RespuestaJson.MiniJson::codigo, Function.identity()));
        } catch (RuntimeException e) {
            // JSON mal formado, sin lista de textos, con un código repetido o sin código
            throw new IllegalStateException("La IA devolvió una redacción ilegible", e);
        }
    }

    private static TextoMiniObjetivo texto(RedaccionObjetivosIaAdapter.RespuestaJson.MiniJson mini) {
        if (mini == null || vacioOLargo(mini.titulo(), RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO)
                || vacioOLargo(mini.justificacion(), RedaccionObjetivosIaAdapter.LARGO_MAXIMO_JUSTIFICACION)) {
            throw new IllegalStateException("La IA no redactó un texto válido para cada renovación");
        }
        return new TextoMiniObjetivo(mini.titulo().strip(), mini.justificacion().strip());
    }

    private static boolean vacioOLargo(String texto, int maximo) {
        return texto == null || texto.isBlank() || texto.strip().length() > maximo;
    }
}
