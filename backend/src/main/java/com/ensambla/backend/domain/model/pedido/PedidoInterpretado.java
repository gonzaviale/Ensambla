package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.util.List;

/**
 * Lo que la IA entendió del mensaje del cliente.
 *
 * @param lineas  lo que pidió; vacío si el mensaje no era un pedido
 * @param consumo lo que costó la consulta, para registrarlo; null si no hubo (modo simulado)
 */
public record PedidoInterpretado(List<LineaPedida> lineas, ConsumoIaSami consumo) {

    public PedidoInterpretado {
        lineas = List.copyOf(lineas);
    }

    public static PedidoInterpretado sinResultado(ConsumoIaSami consumo) {
        return new PedidoInterpretado(List.of(), consumo);
    }
}
