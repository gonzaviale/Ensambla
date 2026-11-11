package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.LineaPedida;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.InterpretePedidoIaPort;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Pasa lo que escribe un cliente a una lista de cosas con un modelo de texto. Una respuesta ilegible no lanza error:
 * vuelve como pedido vacío y con su consumo, para que se registre lo gastado. Sólo falla (con la excepción del cliente)
 * si el modelo no respondió.
 */
@Slf4j
public class InterpretePedidoIaAdapter implements InterpretePedidoIaPort {

    /** Forma del JSON que devuelve el modelo ({@code ia/esquema-whatsapp-pedido.json}), antes de validarlo. */
    record RespuestaJson(List<LineaJson> lineas) {
    }

    record LineaJson(String producto, BigDecimal cantidad) {
    }

    private final ClienteModeloTexto modelo;
    private final PromptWhatsappPedido prompt;
    private final ObjectMapper objectMapper;

    public InterpretePedidoIaAdapter(ClienteModeloTexto modelo, PromptWhatsappPedido prompt,
                                     ObjectMapper objectMapper) {
        this.modelo = modelo;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public PedidoInterpretado interpretar(ConsultaPedido consulta) {
        RespuestaModeloTexto respuesta = modelo.completar(prompt.pedido(consulta));
        ConsumoIaSami consumo = new ConsumoIaSami(respuesta.proveedor(), respuesta.modelo(),
                respuesta.tokensEntrada(), respuesta.tokensSalida());
        try {
            RespuestaJson leida = objectMapper.readerFor(RespuestaJson.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(respuesta.json());
            return traducir(leida, consumo);
        } catch (JacksonException e) {
            log.warn("Sami recibió un JSON ilegible de la IA al interpretar un pedido de WhatsApp: {}", e.getMessage());
            return PedidoInterpretado.sinResultado(consumo);
        }
    }

    private static PedidoInterpretado traducir(RespuestaJson leida, ConsumoIaSami consumo) {
        if (leida == null || leida.lineas() == null) {
            return PedidoInterpretado.sinResultado(consumo);
        }
        List<LineaPedida> lineas = leida.lineas().stream()
                .filter(Objects::nonNull)
                .filter(linea -> linea.producto() != null && !linea.producto().isBlank())
                .map(linea -> new LineaPedida(linea.producto().strip(), linea.cantidad()))
                .toList();
        return new PedidoInterpretado(lineas, consumo);
    }
}
