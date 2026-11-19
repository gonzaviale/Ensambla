package com.ensambla.backend.domain.model.metrica;

import java.util.List;

/**
 * La receta que arma un pedido en palabras, con lo que la persona dijo, lo que se tomó por defecto porque no lo dijo y
 * las palabras que no se entendieron (o piden algo que todavía no se calcula, como «el finde»).
 */
public record PedidoInterpretado(
        DefinicionMetrica receta,
        List<ParteDeLaReceta> delPedido,
        List<ParteDeLaReceta> porDefecto,
        List<String> noEntendidas) {

    public PedidoInterpretado {
        delPedido = List.copyOf(delPedido);
        porDefecto = List.copyOf(porDefecto);
        noEntendidas = List.copyOf(noEntendidas);
    }

    static PedidoInterpretado de(LoQueDiceElPedido loQueDice, String nombre, List<String> noEntendidas) {
        return new PedidoInterpretado(loQueDice.receta(nombre), ParteDeLaReceta.lasQueDijo(loQueDice),
                ParteDeLaReceta.lasDeSiempre(loQueDice), noEntendidas);
    }
}
