package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Map;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_PRECIO;

/** Las acciones que las reglas de avisos de ventas ofrecen sobre un producto: abrirlo o cambiarle el precio. */
final class AccionesAviso {

    private AccionesAviso() {
    }

    static AccionDeAviso abrir(BloqueInventario inventario, RegistroId registroId) {
        return new AccionAbrirRegistro("Ver producto", inventario.bloque().getProyectoId(),
                inventario.bloque().getId(), registroId);
    }

    /** Suma una fila nueva a la tabla de inventario cuando la persona la aplica; los valores van por clave de campo. */
    static AccionDeAviso crearRegistro(BloqueInventario inventario, String texto, Map<String, Object> valores) {
        return new AccionCrearRegistro(texto, inventario.bloque().getProyectoId(), inventario.bloque().getId(),
                valores);
    }

    /** Lleva a la pantalla de la zona, filtrando por un producto; no cambia ningún dato. */
    static AccionDeAviso verZona(String texto, String producto) {
        return new AccionVerZona(texto, producto);
    }

    /** Cambia el precio del producto al sugerido cuando la persona la aplica. */
    static AccionDeAviso cambiarPrecio(BloqueInventario inventario, RegistroId registroId, String texto,
                                       BigDecimal sugerido) {
        return new AccionEditarRegistro(texto, inventario.bloque().getProyectoId(), inventario.bloque().getId(),
                registroId, Map.of(CLAVE_PRECIO, sugerido));
    }

    /** Lleva a la tabla de entradas del proyecto para cargar una. */
    static AccionDeAviso registrarEntrada(BloqueInventario inventario) {
        return new AccionRegistrar("Registrar entrada", inventario.bloque().getProyectoId(),
                inventario.entradasId());
    }
}
