package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;

/** Pasa lo que escribe un cliente («dame 1 kilo de yerba, aceite y fideos») a una lista de cosas con su cantidad. */
public interface InterpretePedidoIaPort {

    /** Un texto que no es un pedido vuelve sin líneas, con su consumo para registrarlo. */
    PedidoInterpretado interpretar(ConsultaPedido consulta);
}
