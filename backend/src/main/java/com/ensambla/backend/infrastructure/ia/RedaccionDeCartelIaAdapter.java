package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.RedaccionDeCartelIa;
import com.ensambla.backend.domain.model.cartel.TextosDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * Redacta los carteles con un modelo de texto. Sólo acepta una respuesta con plantilla conocida y textos que respeten
 * los largos; con cualquier otra cosa lanza una excepción y quien llama usa los textos de reserva.
 */
public class RedaccionDeCartelIaAdapter implements RedaccionDeCartelIaPort {

    /** Forma del JSON que devuelve el modelo ({@code ia/esquema-carteles.json}), antes de validarlo. */
    record RespuestaJson(String plantilla, String titular, String bajada, String textoPosteo) {
    }

    private final ClienteModeloTexto modelo;
    private final PromptCarteles prompt;
    private final ObjectMapper objectMapper;

    public RedaccionDeCartelIaAdapter(ClienteModeloTexto modelo, PromptCarteles prompt, ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public RedaccionDeCartelIa redactar(PedidoDeTextosDeCartel pedido) {
        RespuestaModeloTexto respuesta = modelo.completar(prompt.peticion(pedido));
        return new RedaccionDeCartelIa(textosDe(respuesta.json()), consumoDe(respuesta));
    }

    private TextosDeCartel textosDe(String json) {
        RespuestaJson leida = objectMapper.readerFor(RespuestaJson.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        return new TextosDeCartel(
                TipoDeCartel.valueOf(leida.plantilla()), leida.titular(), leida.bajada(), leida.textoPosteo());
    }

    private ConsumoIaSami consumoDe(RespuestaModeloTexto respuesta) {
        return new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(), respuesta.tokensEntrada(),
                respuesta.tokensSalida());
    }
}
