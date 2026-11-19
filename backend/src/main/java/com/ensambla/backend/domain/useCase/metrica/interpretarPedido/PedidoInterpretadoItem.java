package com.ensambla.backend.domain.useCase.metrica.interpretarPedido;

import com.ensambla.backend.domain.model.metrica.ParteDeLaReceta;
import com.ensambla.backend.domain.model.metrica.PedidoInterpretado;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.useCase.metrica.DefinicionItem;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;

import java.util.List;

/**
 * La métrica que arma un pedido en palabras, con lo que daría hoy y cómo se entendió el pedido.
 *
 * @param yaLaTenes si ya está a la vista (guardada o en el panel de ventas): se muestra pero no se puede guardar
 */
public record PedidoInterpretadoItem(
        DefinicionItem definicion, ResultadoItem resultado, LecturaItem lectura, boolean yaLaTenes) {

    public static PedidoInterpretadoItem de(PedidoInterpretado interpretado, ResultadoMetrica resultado,
                                            boolean yaLaTenes) {
        return new PedidoInterpretadoItem(DefinicionItem.de(interpretado.receta()), ResultadoItem.de(resultado),
                LecturaItem.de(interpretado), yaLaTenes);
    }

    /**
     * @param delPedido    las partes de la receta que la persona dijo
     * @param porDefecto   las que se tomaron por defecto porque el pedido no las decía
     * @param noEntendidas las palabras que no se entendieron, como las escribió la persona
     */
    public record LecturaItem(
            List<ParteDeLaReceta> delPedido, List<ParteDeLaReceta> porDefecto, List<String> noEntendidas) {

        public static LecturaItem de(PedidoInterpretado interpretado) {
            return new LecturaItem(interpretado.delPedido(), interpretado.porDefecto(), interpretado.noEntendidas());
        }
    }
}
