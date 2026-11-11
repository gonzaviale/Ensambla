package com.ensambla.backend.domain.model.pedido;

import java.util.List;

/**
 * Lo que se le pide a la IA: pasar lo que escribió el cliente a una lista de cosas con su cantidad.
 *
 * @param texto     el mensaje del cliente
 * @param productos los nombres de los productos del comercio, para que la IA use el mismo nombre y no invente otro
 */
public record ConsultaPedido(String texto, List<String> productos) {

    public ConsultaPedido {
        productos = List.copyOf(productos);
    }
}
