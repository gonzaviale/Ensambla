package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;

import java.util.List;
import java.util.Map;

/**
 * El id de cada fila que nombran los vínculos de la propuesta, por tabla y nombre.
 *
 * @param sumados las filas que se crearon para poder vincularlas («Marta en Clientes»)
 */
record Vinculados(Map<String, String> ids, List<String> sumados) {

    static String clave(VinculoPropuesto vinculo) {
        return vinculo.bloqueId() + "|" + TextoNormalizado.de(vinculo.nombre());
    }

    /** Los valores de la fila con el id de la fila vinculada en lugar del nombre que escribió la persona. */
    Map<String, Object> ponerIds(FilaPropuesta fila, Map<String, Object> valores) {
        fila.vinculos().forEach((campo, vinculo) -> valores.put(campo, ids.get(clave(vinculo))));
        return valores;
    }
}
