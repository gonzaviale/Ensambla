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

    /** Suma una fila nueva a la tabla de inventario cuando la persona la aplica; los valores van por clave de campo. */
    static Map<String, Object> crearRegistro(BloqueInventario inventario, String texto, Map<String, Object> valores) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "CREAR_REGISTRO");
        accion.put("texto", texto);
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        accion.put("bloqueId", inventario.bloque().getId().toString());
        accion.put("valores", valores);
        return accion;
    }

    /** Lleva a la pantalla de la zona, filtrando por un producto; no cambia ningún dato. */
    static Map<String, Object> verZona(String texto, String producto) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "VER_ZONA");
        accion.put("texto", texto);
        accion.put("producto", producto);
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
