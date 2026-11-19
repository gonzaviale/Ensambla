package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.ParteDeLaReceta;

import java.util.List;

/** La propuesta de siempre (receta y resultado) más cómo se entendió el pedido. */
public record PedidoInterpretadoResponse(
        DefinicionResponse definicion, ResultadoResponse resultado, LecturaResponse lectura) {

    public record LecturaResponse(
            List<ParteDeLaReceta> delPedido, List<ParteDeLaReceta> porDefecto, List<String> noEntendidas) {
    }
}
