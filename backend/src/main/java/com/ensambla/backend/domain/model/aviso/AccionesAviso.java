package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_PRECIO;

/** Las acciones que las reglas de avisos de ventas ofrecen sobre un producto: abrirlo o cambiarle el precio. */
final class AccionesAviso {

    private AccionesAviso() {
    }

    static Map<String, Object> abrir(BloqueInventario inventario, RegistroId registroId) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "ABRIR");
        accion.put("texto", "Ver producto");
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        accion.put("bloqueId", inventario.bloque().getId().toString());
        accion.put("registroId", registroId.toString());
        return accion;
    }

    /** Cambia el precio del producto al sugerido cuando la persona la aplica. */
    static Map<String, Object> cambiarPrecio(BloqueInventario inventario, RegistroId registroId, String texto,
                                             BigDecimal sugerido) {
        Map<String, Object> accion = abrir(inventario, registroId);
        accion.put("tipo", "EDITAR_REGISTRO");
        accion.put("texto", texto);
        accion.put("valores", Map.of(CLAVE_PRECIO, sugerido));
        return accion;
    }
}
