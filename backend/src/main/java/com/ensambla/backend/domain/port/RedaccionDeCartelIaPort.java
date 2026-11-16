package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.RedaccionDeCartelIa;

/**
 * IA que elige la plantilla y redacta el titular, la bajada y el texto del posteo. Sólo redacta: precios, descuentos y
 * fechas salen de los datos reales y la IA no puede cambiarlos.
 */
public interface RedaccionDeCartelIaPort {

    /**
     * Puede lanzar cualquier excepción no verificada (también si la respuesta no sirve); quien llama usa entonces los
     * textos de reserva.
     */
    RedaccionDeCartelIa redactar(PedidoDeTextosDeCartel pedido);
}
