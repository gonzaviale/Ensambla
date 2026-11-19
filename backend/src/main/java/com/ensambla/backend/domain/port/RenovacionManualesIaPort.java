package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;

import java.util.List;

/**
 * IA que redacta la acción que renueva cada mini-objetivo manual leyendo lo que la persona contó. Sólo redacta texto: no
 * decide si se renueva ni cómo (eso lo fija el servidor) y no inventa números.
 */
public interface RenovacionManualesIaPort {

    /**
     * @return un texto por pedido, en el mismo orden; puede lanzar cualquier excepción no verificada (también si la
     *         respuesta no sirve) y quien llama usa entonces el texto de reserva de cada acción
     */
    RedaccionObjetivosIa renovar(List<PedidoRenovacion> pedidos);
}
