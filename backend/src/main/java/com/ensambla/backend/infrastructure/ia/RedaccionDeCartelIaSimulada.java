package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.RedaccionDeCartelIa;
import com.ensambla.backend.domain.model.cartel.TextosDeCartel;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;

/** Proveedor sin costo para desarrollar y probar: redacta con los textos de reserva y no llama a nadie. */
public class RedaccionDeCartelIaSimulada implements RedaccionDeCartelIaPort {

    static final ConsumoIaSami SIN_CONSUMO = new ConsumoIaSami("simulado", "simulado", 0, 0);

    @Override
    public RedaccionDeCartelIa redactar(PedidoDeTextosDeCartel pedido) {
        return new RedaccionDeCartelIa(TextosDeCartel.deReserva(pedido.datos(), pedido.negocio()), SIN_CONSUMO);
    }
}
